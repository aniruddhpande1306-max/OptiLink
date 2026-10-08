import { CheckCircle } from 'lucide-react';

const LaptopReceiverApp = () => {
    return (
        <section>
            <div className="container" style={{ maxWidth: '800px', margin: '0 auto', textAlign: 'center' }}>
                <h2>Laptop Receiver App</h2>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
                    The Windows application captures, processes, and robustly verifies incoming optical matrices dynamically without compromising application state loops.
                </p>
                <ul className="list-styled" style={{ textAlign: 'left', display: 'inline-block' }}>
                    <li><CheckCircle className="list-icon" size={20} /> Captures optical frames through the webcam</li>
                    <li><CheckCircle className="list-icon" size={20} /> Detects and validates incoming frames</li>
                    <li><CheckCircle className="list-icon" size={20} /> Handles frame ordering and duplicates</li>
                    <li><CheckCircle className="list-icon" size={20} /> Reconstructs the original data</li>
                    <li><CheckCircle className="list-icon" size={20} /> Displays the complete received output</li>
                    <li><CheckCircle className="list-icon" size={20} /> Provides Copy, Save and New Transfer options</li>
                    <li><CheckCircle className="list-icon" size={20} /> Works completely offline</li>
                </ul>
            </div>
        </section>
    );
};

export default LaptopReceiverApp;
