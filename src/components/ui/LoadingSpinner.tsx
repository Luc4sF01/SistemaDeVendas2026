import { Loader2 } from 'lucide-react';

interface Props {
  text?: string;
  className?: string;
}

export function LoadingSpinner({ text = 'Carregando...', className = '' }: Props) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 gap-3 text-gray-400 ${className}`}>
      <Loader2 className="animate-spin" size={32} />
      <span className="text-sm">{text}</span>
    </div>
  );
}
