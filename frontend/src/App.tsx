import { Routes, Route } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { HomePage } from './pages/HomePage/HomePage';
import { TierListPage } from './pages/TierListPage/TierListPage';

function App() {
  return (
    <Routes>
      {/* Главная без хедера */}
      <Route path="/" element={<HomePage />} />

      {/* Остальные страницы с хедером */}
      <Route element={<Layout />}>
        <Route path="/tier-list" element={<TierListPage />} />
        {/* Будущие страницы */}
        {/* <Route path="/elo-rating" element={<EloRatingPage />} /> */}
        {/* <Route path="/teammate-battles" element={<TeammateBattlesPage />} /> */}
      </Route>
    </Routes>
  );
}

export default App;
