import cv2
import numpy as np
import struct
import time
import os
import threading
import tkinter as tk
from tkinter import ttk, scrolledtext, filedialog, messagebox
from PIL import Image, ImageTk

GRID_SIZE = 24
HEADER_SIZE = 13

# ------------- CORE ALGORITHM (UNCHANGED) -------------
def calculate_adler32(data: bytes) -> int:
    a = 1
    b = 0
    for byte in data:
        a = (a + byte) % 65521
        b = (b + a) % 65521
    val = ((b << 16) | a) & 0xFFFFFFFF
    if val >= 0x80000000:
        val -= 0x100000000
    return val

def is_finder_pattern(x, y):
    if x < 4 and y < 4: return True
    if x >= GRID_SIZE - 4 and y < 4: return True
    if x < 4 and y >= GRID_SIZE - 4: return True
    return False

def verify_finders(grid_bits):
    def check_finder(col_start, row_start):
        errors = 0
        for y in range(4):
            for x in range(4):
                expected = (x in (1,2) and y in (1,2))
                if grid_bits[row_start+y, col_start+x] != expected:
                    errors += 1
        return errors
    return check_finder(0,0) <= 1 and check_finder(GRID_SIZE-4,0) <= 1 and check_finder(0,GRID_SIZE-4) <= 1

def order_points(pts):
    rect = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]
    return rect

class TransferSession:
    def __init__(self, transfer_id, total_frames):
        self.transfer_id = transfer_id
        self.total_frames = total_frames
        self.frames = {}
        self.completed = False

    def add_frame(self, frame_num, payload):
        if frame_num not in self.frames:
            self.frames[frame_num] = payload
            if len(self.frames) == self.total_frames:
                self.completed = True
                return self.assemble()
        return None

    def assemble(self):
        sorted_payloads = [self.frames[i] for i in range(1, self.total_frames + 1)]
        full_bytes = b"".join(sorted_payloads)
        try:
            return full_bytes.decode('utf-8'), len(full_bytes) # String, byte length
        except UnicodeDecodeError:
            return f"<Decode Error: Binary Data>", len(full_bytes)

# ------------- THREADED OPENCV WORKER -------------
class OpticalReceiverEngine:
    def __init__(self, callback_update_gui, callback_update_video):
        self.callback_update_gui = callback_update_gui
        self.callback_update_video = callback_update_video
        self.running = False
        self.cap = None
        self.camera_index = 0
        self.sessions = {}
        self.current_transfer_id = None
        self.debug_states = self.get_initial_states()
        self.decoded_result = None

    def get_initial_states(self):
        return {
            "cam_status": "Camera: ● Disconnected",
            "status": "Waiting for Phone Screen...",
            "frame_no": 0,
            "total_frames": 0,
            "received": 0,
            "missing": 0,
            "progress": 0.0
        }

    def start(self, camera_index=0):
        self.camera_index = camera_index
        self.running = True
        self.reset_session()
        threading.Thread(target=self._run_loop, daemon=True).start()

    def stop(self):
        self.running = False
        if self.cap:
            self.cap.release()
            self.cap = None

    def reset_session(self):
        self.sessions.clear()
        self.current_transfer_id = None
        self.decoded_result = None
        current_cam = self.debug_states.get("cam_status", "Camera: ● Disconnected")
        self.debug_states = self.get_initial_states()
        self.debug_states["cam_status"] = current_cam
        self.callback_update_gui(self.debug_states, None)

    def _run_loop(self):
        if os.name == 'nt':
            self.cap = cv2.VideoCapture(self.camera_index, cv2.CAP_DSHOW)
            if not self.cap.isOpened():
                print("DirectShow failed, falling back to default VideoCapture.")
                self.cap = cv2.VideoCapture(self.camera_index)
        else:
            self.cap = cv2.VideoCapture(self.camera_index)
            
        if not self.cap.isOpened():
            self.debug_states["cam_status"] = "Camera: ● Disconnected"
            self.debug_states["status"] = "Camera Unavailable"
            self.callback_update_gui(self.debug_states, None)
            return
            
        self.debug_states["cam_status"] = "Camera: ● Connected"

        while self.running:
            ret, frame = self.cap.read()
            if not ret:
                self.debug_states["cam_status"] = "Camera: ● Disconnected"
                self.debug_states["status"] = "Camera Error / Disconnected"
                self.callback_update_gui(self.debug_states, None)
                break

            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            h, w = gray.shape
            al_size = min(h, w) * 3 // 4
            roi_rect = (w//2 - al_size//2, h//2 - al_size//2, al_size, al_size)
            
            if self.debug_states["status"] not in ["Success!", "Receiving Data"]:
                self.debug_states["status"] = "Detecting Optical Frame..."

            best_grid = self.extract_grid_from_roi(gray, roi_rect)
            
            # Live Feed Overlays
            rx, ry, rw, rh = roi_rect
            cv2.rectangle(frame, (rx, ry), (rx+rw, ry+rh), (255, 0, 0), 2)
            if best_grid is not None:
                disp_grid = best_grid + [rx, ry]
                cv2.polylines(frame, [disp_grid], True, (0, 255, 0), 3)

            # Route to UI
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            if self.running:
                self.callback_update_video(frame_rgb)
                self.callback_update_gui(self.debug_states, self.decoded_result)

            time.sleep(0.01)

        if self.cap:
            self.cap.release()
            self.cap = None

    def process_grid(self, grid_bits):
        data_bits = []
        for y in range(GRID_SIZE):
            for x in range(GRID_SIZE):
                if not is_finder_pattern(x, y):
                    data_bits.append(grid_bits[y, x])
        
        if len(data_bits) < HEADER_SIZE * 8: return False

        byte_array = bytearray()
        for i in range(0, len(data_bits) - 7, 8):
            b = 0
            for j in range(8):
                if data_bits[i + j]:
                    b |= (1 << (7 - j))
            byte_array.append(b)

        if len(byte_array) < HEADER_SIZE: return False

        try:
            tid = struct.unpack('>i', byte_array[0:4])[0]
            fnum = struct.unpack('>h', byte_array[4:6])[0]
            tnum = struct.unpack('>h', byte_array[6:8])[0]
            plen = byte_array[8]
            chk = struct.unpack('>i', byte_array[9:13])[0]
        except struct.error: return False

        if plen > len(byte_array) - 13: return False
        if fnum <= 0 or tnum <= 0 or fnum > tnum or plen < 0: return False

        payload = bytes(byte_array[13:13+plen])
        calc_chk = calculate_adler32(payload)
        
        if calc_chk != chk:
            return False

        if tid not in self.sessions:
            self.sessions[tid] = TransferSession(tid, tnum)
        
        self.current_transfer_id = tid
        session = self.sessions[tid]
        
        result = session.add_frame(fnum, payload)
        
        num_valid = len(session.frames)
        missing = tnum - num_valid
        prog = (num_valid / tnum) * 100
        
        self.debug_states["frame_no"] = fnum
        self.debug_states["total_frames"] = tnum
        self.debug_states["received"] = num_valid
        self.debug_states["missing"] = missing
        self.debug_states["progress"] = prog
        self.debug_states["status"] = "Success!" if result else "Receiving Data"

        if result:
            text_str, byte_len = result
            self.decoded_result = {
                "text": text_str,
                "bytes": byte_len,
                "chars": len(text_str),
                "frames": tnum
            }

        return True

    def extract_grid_from_roi(self, img_gray, roi_rect):
        x, y, w, h = roi_rect
        roi = img_gray[y:y+h, x:x+w]
        if roi.size == 0: return None
        
        thresh = cv2.adaptiveThreshold(roi, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 21, 2)
        contours, _ = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        
        max_area = 0
        best_grid = None
        roi_area = w * h
        
        for cnt in contours:
            area = cv2.contourArea(cnt)
            if area > roi_area * 0.1:
                peri = cv2.arcLength(cnt, True)
                approx = cv2.approxPolyDP(cnt, 0.04 * peri, True)
                if len(approx) == 4:
                    rect = cv2.boundingRect(approx)
                    aspect = float(rect[2]) / rect[3] if rect[3] > 0 else 0
                    if 0.7 <= aspect <= 1.3:
                        if area > max_area:
                            max_area = area
                            best_grid = approx

        if best_grid is not None:
            pts = best_grid.reshape(4, 2)
            rect = order_points(pts)
            
            TARGET_SIZE = 260
            dst = np.array([
                [0, 0],
                [TARGET_SIZE - 1, 0],
                [TARGET_SIZE - 1, TARGET_SIZE - 1],
                [0, TARGET_SIZE - 1]
            ], dtype="float32")
            
            M = cv2.getPerspectiveTransform(rect, dst)
            warped = cv2.warpPerspective(roi, M, (TARGET_SIZE, TARGET_SIZE))
            
            grid_only = warped[10:250, 10:250]
            _, warped_thresh = cv2.threshold(grid_only, 127, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
            
            CELL = 10 
            grid_bits = np.zeros((GRID_SIZE, GRID_SIZE), dtype=bool)
            
            for gy in range(GRID_SIZE):
                for gx in range(GRID_SIZE):
                    cy = gy * CELL + CELL // 2
                    cx = gx * CELL + CELL // 2
                    window = warped_thresh[max(0, cy-2):cy+3, max(0, cx-2):cx+3]
                    if window.size > 0:
                        grid_bits[gy, gx] = np.median(window) > 127
                    else:
                        grid_bits[gy, gx] = warped_thresh[cy, cx] > 127
                        
            for rot in range(4):
                if verify_finders(grid_bits):
                    if self.process_grid(grid_bits):
                        return best_grid
                    return best_grid
                grid_bits = np.rot90(grid_bits)
                
        return None

# ------------- TKINTER UI -------------
class Application:
    def __init__(self, root):
        self.root = root
        self.root.title("OptiLink Receiver — Offline Optical Data Receiver")
        self.root.geometry("1100x750")
        self.root.configure(bg="#f4f6f9")
        self.root.minsize(900, 600)

        style = ttk.Style()
        style.theme_use('clam')
        style.configure("TLabel", background="#f4f6f9", font=("Segoe UI", 10))
        style.configure("Header.TLabel", font=("Segoe UI", 24, "bold"), foreground="#2c3e50")
        style.configure("Subhead.TLabel", font=("Segoe UI", 12, "bold"), foreground="#34495e")
        style.configure("Success.TLabel", font=("Segoe UI", 16, "bold"), foreground="#27ae60")
        
        self.engine = OpticalReceiverEngine(self.update_gui_stats, self.update_video)
        
        self.create_widgets()

    def create_widgets(self):
        main_container = tk.Frame(self.root, bg="#f4f6f9", padx=20, pady=20)
        main_container.pack(fill=tk.BOTH, expand=True)

        header_frame = tk.Frame(main_container, bg="#f4f6f9")
        header_frame.pack(fill=tk.X, pady=(0, 20))
        ttk.Label(header_frame, text="OptiLink", style="Header.TLabel").pack(side=tk.LEFT)
        ttk.Label(header_frame, text="Offline Optical Data Receiver", style="TLabel").pack(side=tk.LEFT, padx=15, pady=10)

        body_frame = tk.Frame(main_container, bg="#f4f6f9")
        body_frame.pack(fill=tk.BOTH, expand=True)

        # LEFT COLUMN
        left_col = tk.Frame(body_frame, bg="#f4f6f9", width=420)
        left_col.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 20))
        left_col.pack_propagate(False) # Optional, but we'll focus on just the video container directly

        self.video_container = tk.Frame(left_col, width=400, height=300, bg="black")
        self.video_container.pack(pady=(0, 15))
        self.video_container.pack_propagate(False) # Prevents the container from collapsing around the label!

        self.video_label = tk.Label(self.video_container, bg="black", text="Camera Offline", fg="white")
        self.video_label.pack(expand=True, fill=tk.BOTH)

        cam_frame = tk.Frame(left_col, bg="#f4f6f9")
        cam_frame.pack(fill=tk.X, pady=5)
        ttk.Label(cam_frame, text="Camera Source:").pack(side=tk.LEFT)
        self.cam_combo = ttk.Combobox(cam_frame, values=["0", "1", "2"], width=5, state="readonly")
        self.cam_combo.current(0)
        self.cam_combo.pack(side=tk.LEFT, padx=10)

        btn_frame = tk.Frame(left_col, bg="#f4f6f9")
        btn_frame.pack(fill=tk.X, pady=15)
        self.btn_start = ttk.Button(btn_frame, text="Start Receiver", command=self.start_receiver)
        self.btn_start.pack(side=tk.LEFT, padx=(0,5), fill=tk.X, expand=True)
        self.btn_stop = ttk.Button(btn_frame, text="Stop Receiver", command=self.stop_receiver, state=tk.DISABLED)
        self.btn_stop.pack(side=tk.LEFT, padx=(5,0), fill=tk.X, expand=True)

        status_card = tk.Frame(left_col, bg="#ffffff", bd=1, relief=tk.SOLID, padx=15, pady=15)
        status_card.pack(fill=tk.X, pady=10)
        self.lbl_camera_status = ttk.Label(status_card, text="Camera: ● Disconnected", background="#ffffff")
        self.lbl_camera_status.pack(anchor=tk.W, pady=(0, 5))
        self.lbl_overall = ttk.Label(status_card, text="Status: Ready", background="#ffffff", foreground="#2980b9", font=("Segoe UI", 10, "bold"))
        self.lbl_overall.pack(anchor=tk.W)

        # RIGHT COLUMN
        right_col = tk.Frame(body_frame, bg="#f4f6f9")
        right_col.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        stats_card = tk.Frame(right_col, bg="#ffffff", bd=1, relief=tk.SOLID, padx=20, pady=20)
        stats_card.pack(fill=tk.X, pady=(0, 20))
        
        ttk.Label(stats_card, text="TRANSFER STATUS", style="Subhead.TLabel", background="#ffffff").pack(anchor=tk.W, pady=(0, 15))
        
        info_frame1 = tk.Frame(stats_card, bg="#ffffff")
        info_frame1.pack(fill=tk.X)
        self.lbl_tid = ttk.Label(info_frame1, text="Transfer ID: -", background="#ffffff")
        self.lbl_tid.pack(side=tk.LEFT, fill=tk.X, expand=True)
        ttk.Label(info_frame1, text="Mode: Custom Optical", background="#ffffff").pack(side=tk.LEFT, fill=tk.X, expand=True)

        info_frame2 = tk.Frame(stats_card, bg="#ffffff")
        info_frame2.pack(fill=tk.X, pady=10)
        self.lbl_received = ttk.Label(info_frame2, text="Received: 0 / 0", background="#ffffff")
        self.lbl_received.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.lbl_missing = ttk.Label(info_frame2, text="Missing: 0", background="#ffffff")
        self.lbl_missing.pack(side=tk.LEFT, fill=tk.X, expand=True)

        self.progress_var = tk.DoubleVar()
        self.prog_bar = ttk.Progressbar(stats_card, variable=self.progress_var, maximum=100)
        self.prog_bar.pack(fill=tk.X, pady=15)
        self.lbl_prog_val = ttk.Label(stats_card, text="Progress: 0.0%", background="#ffffff")
        self.lbl_prog_val.pack()

        # Success Banner
        self.lbl_success = ttk.Label(right_col, text="✓ TRANSFER SUCCESSFUL", style="Success.TLabel")
        self.lbl_final_stats = ttk.Label(right_col, text="", style="TLabel")

        ttk.Label(right_col, text="DECODED DATA", style="Subhead.TLabel").pack(anchor=tk.W, pady=(0, 10))
        # Output Actions
        out_action_frame = tk.Frame(right_col, bg="#f4f6f9")
        out_action_frame.pack(side=tk.BOTTOM, fill=tk.X, pady=15)
        
        self.btn_copy = tk.Button(out_action_frame, text="Copy All", command=self.copy_all, width=15, font=("Segoe UI", 10))
        self.btn_copy.pack(side=tk.LEFT, padx=(0, 10))
        
        self.btn_save = tk.Button(out_action_frame, text="Save TXT", command=self.save_txt, width=15, font=("Segoe UI", 10))
        self.btn_save.pack(side=tk.LEFT, padx=10)
        
        self.btn_new = tk.Button(out_action_frame, text="New Transfer", command=self.new_transfer, width=15, font=("Segoe UI", 10))
        self.btn_new.pack(side=tk.RIGHT)
        
        # Text Output (Placed AFTER so it fills the middle natively!)
        self.txt_out = scrolledtext.ScrolledText(right_col, wrap=tk.WORD, state=tk.DISABLED, font=("Consolas", 10))
        self.txt_out.pack(side=tk.TOP, fill=tk.BOTH, expand=True)
        
        self.transfer_completed_shown = False

    def start_receiver(self):
        c_idx = int(self.cam_combo.get())
        self.btn_start.config(state=tk.DISABLED)
        self.btn_stop.config(state=tk.NORMAL)
        # We no longer optimistically say it is linked. Engine will update it securely!
        self.clear_output()
        self.lbl_success.pack_forget()
        self.lbl_final_stats.pack_forget()
        self.engine.start(c_idx)

    def stop_receiver(self):
        self.engine.stop()
        self.btn_start.config(state=tk.NORMAL)
        self.btn_stop.config(state=tk.DISABLED)
        self.lbl_camera_status.config(text="Camera: ● Disconnected", foreground="#e74c3c")
        self.lbl_overall.config(text="Status: Stopped", foreground="#e74c3c")
        self.video_label.config(image='')

    def new_transfer(self):
        self.clear_output()
        self.engine.reset_session()
        self.transfer_completed_shown = False
        self.lbl_success.pack_forget()
        self.lbl_final_stats.pack_forget()
        self.progress_var.set(0)
        self.lbl_prog_val.config(text="Progress: 0.0%")
        self.lbl_received.config(text="Received: 0 / 0")
        self.lbl_missing.config(text="Missing: 0")
        self.lbl_overall.config(text="Status: Ready for new transfer", foreground="#2980b9")

    def update_video(self, frame_rgb):
        try:
            img = Image.fromarray(frame_rgb)
            img.thumbnail((380, 280))
            imgtk = ImageTk.PhotoImage(image=img)
            self.video_label.imgtk = imgtk
            self.video_label.configure(image=imgtk)
        except Exception:
            pass

    def update_gui_stats(self, states, decoded):
        self.root.after(0, self._update_gui_main_thread, states, decoded)
        
    def _update_gui_main_thread(self, states, decoded):
        self.lbl_overall.config(text=f"Status: {states['status']}")
        
        if "cam_status" in states:
            if "Connected" in states["cam_status"]:
                self.lbl_camera_status.config(text=states["cam_status"], foreground="#27ae60")
            elif "Disconnected" in states["cam_status"]:
                self.lbl_camera_status.config(text=states["cam_status"], foreground="#e74c3c")
                
        if self.engine.current_transfer_id is not None:
            self.lbl_tid.config(text=f"Transfer ID: {self.engine.current_transfer_id}")
            self.lbl_received.config(text=f"Received: {states['received']} / {states['total_frames']}")
            self.lbl_missing.config(text=f"Missing: {states['missing']}")
            self.progress_var.set(states['progress'])
            self.lbl_prog_val.config(text=f"Progress: {states['progress']:.2f}%")

        if decoded is not None and not self.transfer_completed_shown:
            self.transfer_completed_shown = True
            
            self.lbl_success.pack(before=self.txt_out, pady=(5,0), anchor=tk.W)
            stat_text = f"Total Frames: {decoded['frames']}  |  Received Frames: {decoded['frames']}  |  Missing Frames: 0\nCharacters: {decoded['chars']}  |  UTF-8 Bytes: {decoded['bytes']}"
            self.lbl_final_stats.config(text=stat_text)
            self.lbl_final_stats.pack(before=self.txt_out, anchor=tk.W, pady=(0,10))

            self.txt_out.config(state=tk.NORMAL)
            self.txt_out.delete(1.0, tk.END)
            self.txt_out.insert(tk.END, decoded['text'])
            self.txt_out.config(state=tk.DISABLED)

    def copy_all(self):
        val = self.txt_out.get(1.0, tk.END).rstrip('\n')
        if not val:
            messagebox.showinfo("Empty", "No decoded data to copy.")
            return
        self.root.clipboard_clear()
        self.root.clipboard_append(val)
        self.root.update()
        messagebox.showinfo("Copied", "Copied to clipboard.")

    def save_txt(self):
        val = self.txt_out.get(1.0, tk.END).rstrip('\n')
        if not val:
            messagebox.showinfo("Empty", "No decoded data to save.")
            return
        f = filedialog.asksaveasfilename(defaultextension=".txt", initialfile="OptiLink_Received.txt", filetypes=[("Text Files", "*.txt")])
        if f:
            with open(f, 'w', encoding='utf-8') as out:
                out.write(val)
            messagebox.showinfo("Saved", "Saved successfully.")

    def clear_output(self):
        self.txt_out.config(state=tk.NORMAL)
        self.txt_out.delete(1.0, tk.END)
        self.txt_out.config(state=tk.DISABLED)

if __name__ == "__main__":
    root = tk.Tk()
    app = Application(root)
    root.mainloop()
