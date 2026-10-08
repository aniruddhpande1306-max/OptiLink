import { FileText, Smartphone, Webcam, Monitor, BoxSelect } from 'lucide-react';

const HowItWorks = () => {
    return (
        <section id="how-it-works">
            <div className="container">
                <h2>How It Works</h2>
                <div className="flow-diagram">
                    <div className="flow-step">
                        <FileText size={20} className="list-icon" /> Input Data
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <BoxSelect size={20} className="list-icon" /> Optical Encoding
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <Smartphone size={20} className="list-icon" /> Mobile Display
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step" style={{ borderColor: 'var(--accent-primary)', color: 'var(--accent-primary)' }}>
                        Visible Light Transfer
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <Webcam size={20} className="list-icon" /> Laptop Webcam
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <BoxSelect size={20} className="list-icon" /> Optical Decoding
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <Activity size={20} className="list-icon" /> Data Reconstruction
                    </div>
                    <div className="flow-arrow"></div>

                    <div className="flow-step">
                        <Monitor size={20} className="list-icon" /> Original Output
                    </div>
                </div>
            </div>
        </section>
    );
};

import { Activity } from 'lucide-react';
export default HowItWorks;
