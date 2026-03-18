import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ShoppingCart,
  Package,
  Warehouse,
  Users,
  ClipboardList,
  BarChart2,
  PawPrint,
} from 'lucide-react';
import { clsx } from 'clsx';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard', end: true },
  { to: '/nova-venda', icon: ShoppingCart, label: 'Nova Venda' },
  { to: '/produtos', icon: Package, label: 'Produtos' },
  { to: '/estoque', icon: Warehouse, label: 'Estoque' },
  { to: '/clientes', icon: Users, label: 'Clientes' },
  { to: '/historico', icon: ClipboardList, label: 'Histórico' },
  { to: '/relatorios', icon: BarChart2, label: 'Relatórios' },
];

export function Sidebar() {
  return (
    <aside className="w-60 min-h-screen flex flex-col text-white shadow-xl flex-shrink-0"
      style={{ background: 'linear-gradient(180deg, #4C1D95 0%, #6D28D9 60%, #5B21B6 100%)' }}>
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-purple-700/50">
        <div className="w-9 h-9 rounded-xl flex items-center justify-center shadow-lg"
          style={{ background: 'linear-gradient(135deg, #16A34A, #15803d)' }}>
          <PawPrint size={20} className="text-white" />
        </div>
        <div>
          <span className="font-bold text-lg tracking-tight leading-none">PetsTop</span>
          <p className="text-purple-300 text-[10px] font-medium tracking-widest uppercase">Pet Shop</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 px-3 space-y-0.5">
        {navItems.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150',
                isActive
                  ? 'bg-white text-primary shadow-md'
                  : 'text-purple-200 hover:bg-purple-700/50 hover:text-white'
              )
            }
          >
            {({ isActive }) => (
              <>
                <span className={clsx(
                  'w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 transition-colors',
                  isActive ? 'bg-primary-100' : 'bg-purple-700/40'
                )}>
                  <Icon size={15} />
                </span>
                {label}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-4 py-3 border-t border-purple-700/50 text-[11px] text-purple-400 text-center">
        PetsTop v1.0 · Sistema de Vendas
      </div>
    </aside>
  );
}
