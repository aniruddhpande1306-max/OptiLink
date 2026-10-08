import { CheckCircle } from 'lucide-react';

const KeyFeatures = () => {
    return (
        <div>
            <h2>Key Features</h2>
            <ul className="list-styled" style={{ marginTop: '2rem' }}>
                <li><CheckCircle className="list-icon" size={24} /> Offline Data Transfer</li>
                <li><CheckCircle className="list-icon" size={24} /> Visible-Light Communication</li>
                <li><CheckCircle className="list-icon" size={24} /> No Network Dependency</li>
                <li><CheckCircle className="list-icon" size={24} /> Custom Optical Encoding</li>
                <li><CheckCircle className="list-icon" size={24} /> Multi-Frame Data Reconstruction</li>
            </ul>
        </div>
    );
};

export default KeyFeatures;
