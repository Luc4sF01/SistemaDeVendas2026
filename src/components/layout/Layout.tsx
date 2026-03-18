import { type ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { useLocation } from 'react-router-dom';

const pageTitles: Record<string, { title: string; sub: string }> = {
  '/': { title: 'Dashboard', sub: 'Visão geral do seu pet shop' },
  '/nova-venda': { title: 'Nova Venda', sub: 'Registre uma nova venda no PDV' },
  '/produtos': { title: 'Produtos', sub: 'Gerencie o catálogo de produtos' },
  '/estoque': { title: 'Estoque', sub: 'Controle e ajuste de inventário' },
  '/clientes': { title: 'Clientes', sub: 'Cadastro de clientes' },
  '/historico': { title: 'Histórico de Vendas', sub: 'Consulte vendas realizadas' },
  '/relatorios': { title: 'Relatórios', sub: 'Análises e exportações' },
};

export function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const page = pageTitles[location.pathname] ?? { title: 'PetsTop', sub: '' };

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar />

      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header */}
        <header className="bg-white border-b border-purple-100 px-6 py-3.5 flex items-center shadow-sm">
          <div>
            <h1 className="text-base font-bold text-gray-900 leading-tight">{page.title}</h1>
            {page.sub && <p className="text-xs text-gray-400 mt-0.5">{page.sub}</p>}
          </div>
          <div className="ml-auto flex items-center gap-2">
            <span className="text-xs text-gray-400 hidden sm:block">PetsTop · Pet Shop</span>
            <div className="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold">P</div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-6 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
