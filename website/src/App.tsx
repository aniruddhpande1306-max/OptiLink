import Navigation from './components/Navigation';
import WhatIsOptiLink from './components/WhatIsOptiLink';
import HowItWorks from './components/HowItWorks';
import MobileSenderApp from './components/MobileSenderApp';
import LaptopReceiverApp from './components/LaptopReceiverApp';
import OpticalEncoding from './components/OpticalEncoding';
import ApplicationAccess from './components/ApplicationAccess';
import KeyFeatures from './components/KeyFeatures';
import FutureScope from './components/FutureScope';
import Footer from './components/Footer';

function App() {
  return (
    <>
      <Navigation />
      <main>
        <WhatIsOptiLink />
        <HowItWorks />
        <MobileSenderApp />
        <LaptopReceiverApp />
        <OpticalEncoding />
        <ApplicationAccess />
        <div className="container" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4rem', padding: '6rem 0', borderBottom: '1px solid var(--border-color)' }}>
          <KeyFeatures />
          <FutureScope />
        </div>
      </main>
      <Footer />
    </>
  );
}

export default App;
