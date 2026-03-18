import { type ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { useLocation } from 'react-router-dom';

const pageTitles: Record<string, string> = {
  '/': 'Dashboard',
  '/nova-venda': 'Nova Venda',
  '/produtos': 'Produtos',
  '/estoque': 'Estoque',
  '/clientes': 'Clientes',
  '/historico': 'Histórico de Vendas',
  '/relatorios': 'Relatórios',
};

export function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const title = pageTitles[location.pathname] ?? 'VendasPET';

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar />

      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header */}
        <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center shadow-sm">
          <h1 className="text-lg font-semibold text-gray-800">{title}</h1>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-6 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
