import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { Dashboard } from './pages/Dashboard';
import { NovaVenda } from './pages/NovaVenda';
import { Produtos } from './pages/Produtos';
import { Estoque } from './pages/Estoque';
import { Clientes } from './pages/Clientes';
import { HistoricoVendas } from './pages/HistoricoVendas';
import { Relatorios } from './pages/Relatorios';

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/nova-venda" element={<NovaVenda />} />
        <Route path="/produtos" element={<Produtos />} />
        <Route path="/estoque" element={<Estoque />} />
        <Route path="/clientes" element={<Clientes />} />
        <Route path="/historico" element={<HistoricoVendas />} />
        <Route path="/relatorios" element={<Relatorios />} />
      </Routes>
    </Layout>
  );
}

export default App;
