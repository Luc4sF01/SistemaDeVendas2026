import { clsx } from 'clsx';
import { type ReactNode } from 'react';

type Color = 'green' | 'red' | 'red-light' | 'yellow' | 'blue' | 'gray';

const colorMap: Record<Color, string> = {
  green: 'bg-success-light text-success border border-green-200',
  red: 'bg-red-100 text-danger border border-red-300 font-semibold',
  'red-light': 'bg-red-50 text-red-600 border border-red-200',
  yellow: 'bg-warning-light text-warning border border-yellow-200',
  blue: 'bg-primary-50 text-primary border border-blue-200',
  gray: 'bg-gray-100 text-gray-600 border border-gray-200',
};

interface Props {
  color?: Color;
  children: ReactNode;
  className?: string;
}

export function Badge({ color = 'gray', children, className }: Props) {
  return (
    <span
      className={clsx(
        'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium',
        colorMap[color],
        className
      )}
    >
      {children}
    </span>
  );
}
