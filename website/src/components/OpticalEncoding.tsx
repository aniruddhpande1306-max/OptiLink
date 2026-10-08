const OpticalEncoding = () => {
    return (
        <section id="technology">
            <div className="container">
                <h2>Custom 2D Optical Frame</h2>
                <div className="tech-grid">
                    <div className="card">
                        <h3>Matrix Structure</h3>
                        <p className="text-secondary text-muted">24 × 24 logical grid mapping binary points deterministically establishing standard patterns.</p>
                    </div>
                    <div className="card">
                        <h3>Identifier Block</h3>
                        <p className="text-secondary text-muted">Transfer ID mapping isolated transmission structures rejecting old sessions.</p>
                    </div>
                    <div className="card">
                        <h3>Frame Numbering</h3>
                        <p className="text-secondary text-muted">Strict positional numbering handling un-ordered collections sequentially.</p>
                    </div>
                    <div className="card">
                        <h3>Payload Segments</h3>
                        <p className="text-secondary text-muted">Payload information carrying highly condensed raw byte streams natively.</p>
                    </div>
                    <div className="card">
                        <h3>Integrity Pass</h3>
                        <p className="text-secondary text-muted">Checksum validation strictly filtering environmental noise discarding false flags.</p>
                    </div>
                    <div className="card">
                        <h3>Byte Compounding</h3>
                        <p className="text-secondary text-muted">UTF-8 data handling securely combining multi-frame transmission bytes independently.</p>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default OpticalEncoding;
