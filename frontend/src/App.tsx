import {Routes, Route} from 'react-router-dom';
import {HomePage} from './pages/HomePage/HomePage';
import {TierListPage} from './pages/TierListPage/TierListPage';

function App() {
    return (
        <Routes>
            <Route path="/" element={<HomePage/>}/>
            <Route path="/tier-list" element={<TierListPage/>}/>
        </Routes>
    );
}

export default App;