import { ArrowRightCircle } from 'lucide-react';

const FutureScope = () => {
    return (
        <div>
            <h2>Future Scope</h2>
            <ul className="list-styled" style={{ marginTop: '2rem' }}>
                <li><ArrowRightCircle className="list-icon" size={24} /> Higher-Density Optical Frames</li>
                <li><ArrowRightCircle className="list-icon" size={24} /> Faster Transmission</li>
                <li><ArrowRightCircle className="list-icon" size={24} /> Advanced Error Correction</li>
                <li><ArrowRightCircle className="list-icon" size={24} /> File-Level Transfer</li>
                <li><ArrowRightCircle className="list-icon" size={24} /> Improved Camera-Based Detection</li>
            </ul>
        </div>
    );
};

export default FutureScope;
