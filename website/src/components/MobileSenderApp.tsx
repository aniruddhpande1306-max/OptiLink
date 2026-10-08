import { CheckCircle } from 'lucide-react';

const MobileSenderApp = () => {
    return (
        <section id="applications" style={{ borderBottom: 'none', paddingBottom: '0' }}>
            <div className="container" style={{ maxWidth: '800px', margin: '0 auto', textAlign: 'center' }}>
                <h2>Mobile Sender App</h2>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
                    The Android application acts as the secure offline transmitter, converting raw user inputs securely into optical data structures dynamically.
                </p>
                <ul className="list-styled" style={{ textAlign: 'left', display: 'inline-block' }}>
                    <li><CheckCircle className="list-icon" size={20} /> Converts data into optical frames</li>
                    <li><CheckCircle className="list-icon" size={20} /> Adds frame information and error-checking data</li>
                    <li><CheckCircle className="list-icon" size={20} /> Displays frames sequentially through the phone screen</li>
                    <li><CheckCircle className="list-icon" size={20} /> Shows transmission progress and completion status</li>
                    <li><CheckCircle className="list-icon" size={20} /> Works completely offline</li>
                </ul>
            </div>
        </section>
    );
};

export default MobileSenderApp;
