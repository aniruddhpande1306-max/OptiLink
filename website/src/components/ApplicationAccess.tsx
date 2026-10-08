import { AppWindow, Lock } from 'lucide-react';

const ApplicationAccess = () => {
    return (
        <section id="application-access" style={{ backgroundColor: 'var(--bg-main)' }}>
            <div className="container" style={{ maxWidth: '900px', margin: '0 auto', textAlign: 'center' }}>
                <h2>Application Access</h2>

                <div className="card" style={{ marginTop: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem', backgroundColor: 'var(--bg-surface-elevated)' }}>
                    <div style={{ display: 'flex', gap: '1rem', color: 'var(--text-muted)' }}>
                        <AppWindow size={32} />
                        <Lock size={32} />
                    </div>
                    <h3 style={{ marginBottom: '0.5rem' }}>Mobile Sender App & Laptop Receiver App</h3>
                    <p className="text-secondary" style={{ maxWidth: '750px', margin: '0 auto', lineHeight: '1.8' }}>
                        Both applications are fully functional and were demonstrated in the submitted project video. No downloadable links are provided, as both applications are designed to work completely offline without Wi-Fi, Internet, or network communication.
                    </p>
                </div>
            </div>
        </section>
    );
};

export default ApplicationAccess;
