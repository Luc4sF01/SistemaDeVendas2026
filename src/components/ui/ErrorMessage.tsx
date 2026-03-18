import { AlertCircle, RefreshCw } from 'lucide-react';

interface Props {
  message?: string;
  onRetry?: () => void;
}

export function ErrorMessage({ message = 'Erro ao carregar dados.', onRetry }: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3 text-danger">
      <AlertCircle size={32} />
      <p className="text-sm font-medium">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="flex items-center gap-1 text-xs text-primary hover:underline"
        >
          <RefreshCw size={12} /> Tentar novamente
        </button>
      )}
    </div>
  );
}
