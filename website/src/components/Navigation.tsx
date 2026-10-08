import { Activity } from 'lucide-react';

const Navigation = () => {
    return (
        <nav className="navbar">
            <div className="nav-container">
                <a href="#" className="nav-logo">
                    <Activity size={24} color="var(--accent-primary)" />
                    OptiLink
                </a>
                <ul className="nav-links">
                    <li><a href="#what-is-optilink">What It Is</a></li>
                    <li><a href="#how-it-works">How It Works</a></li>
                    <li><a href="#applications">Applications</a></li>
                    <li><a href="#technology">Technology</a></li>
                    <li><a href="#application-access">Application Access</a></li>
                </ul>
            </div>
        </nav>
    );
};

export default Navigation;
