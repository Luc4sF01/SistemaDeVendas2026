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
    <aside className="w-56 min-h-screen bg-primary-800 flex flex-col text-white shadow-xl flex-shrink-0">
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-primary-700">
        <div className="w-8 h-8 bg-white rounded-lg flex items-center justify-center">
          <PawPrint className="text-primary" size={18} />
        </div>
        <span className="font-bold text-lg tracking-tight">VendasPET</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 px-2 space-y-0.5">
        {navItems.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
                isActive
                  ? 'bg-white text-primary shadow-sm'
                  : 'text-blue-100 hover:bg-primary-700 hover:text-white'
              )
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-4 py-3 border-t border-primary-700 text-xs text-blue-300 text-center">
        VendasPET v1.0
      </div>
    </aside>
  );
}
