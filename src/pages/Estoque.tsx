import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Package, AlertTriangle, XCircle, CheckCircle, Edit3,
  Upload, Download, FileDown, FileText,
} from 'lucide-react';
import { produtosService } from '../services/produtosService';
import { StatCard } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import type { Produto } from '../types';
import {
  formatCurrency, getEstoqueBadge,
  exportarEstoqueCSV, gerarModeloCSV, parsearCSVEstoque,
  exportarPDF,
} from '../utils';

interface ImportResult {
  nome: string;
  status: 'ok' | 'erro' | 'pendente';
  msg: string;
}

export function Estoque() {
  const qc = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selected, setSelected] = useState<Produto | null>(null);
  const [novaQty, setNovaQty] = useState('');
  const [modalAjuste, setModalAjuste] = useState(false);
  const [modalImport, setModalImport] = useState(false);
  const [importResults, setImportResults] = useState<ImportResult[]>([]);
  const [importErros, setImportErros] = useState<string[]>([]);
  const [importando, setImportando] = useState(false);

  const { data: produtos, isLoading, isError, refetch } = useQuery({
    queryKey: ['produtos'],
    queryFn: produtosService.listar,
  });

  const ajusteMutation = useMutation({
    mutationFn: ({ id, qty }: { id: number; qty: number }) =>
      produtosService.atualizarEstoque(id, qty),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['produtos'] });
      setModalAjuste(false);
      setSelected(null);
    },
  });

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage onRetry={refetch} />;

  const total = produtos?.length ?? 0;
  const zerados = produtos?.filter((p) => p.quantidadeEstoque === 0).length ?? 0;
  const criticos = produtos?.filter((p) => p.quantidadeEstoque > 0 && p.quantidadeEstoque <= 3).length ?? 0;
  const baixos = produtos?.filter((p) => p.quantidadeEstoque > 3 && p.quantidadeEstoque <= 10).length ?? 0;

  function openAjuste(p: Produto) {
    setSelected(p);
    setNovaQty(String(p.quantidadeEstoque));
    setModalAjuste(true);
  }

  // ── CSV Export ───────────────────────────────────────────────────────────────

  function handleExportCSV() {
    if (produtos) exportarEstoqueCSV(produtos);
  }

  function handleExportPDF() {
    if (!produtos) return;
    const linhas = produtos.map((p) => {
      const { label } = getEstoqueBadge(p.quantidadeEstoque);
      const badgeClass = p.quantidadeEstoque === 0 ? 'badge-red'
        : p.quantidadeEstoque <= 3 ? 'badge-red'
        : p.quantidadeEstoque <= 10 ? 'badge-yellow'
        : 'badge-green';
      return `<tr>
        <td>#${p.id}</td>
        <td><strong>${p.nome}</strong></td>
        <td>${p.categoria}</td>
        <td>${formatCurrency(p.preco)}</td>
        <td style="text-align:center;font-weight:bold">${p.quantidadeEstoque}</td>
        <td><span class="${badgeClass}">${label}</span></td>
      </tr>`;
    }).join('');

    const html = `
      <h2>Inventário Completo (${produtos.length} produtos)</h2>
      <table>
        <thead><tr><th>ID</th><th>Nome</th><th>Categoria</th><th>Preço</th><th>Estoque</th><th>Situação</th></tr></thead>
        <tbody>${linhas}</tbody>
      </table>
      <p style="font-size:11px;color:#888;margin-top:8px">
        Zerados: ${zerados} · Críticos: ${criticos} · Baixos: ${baixos} · OK: ${total - zerados - criticos - baixos}
      </p>
    `;
    exportarPDF('Relatório de Estoque', html);
  }

  // ── CSV Import ───────────────────────────────────────────────────────────────

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (ev) => {
      const texto = ev.target?.result as string;
      const { linhas, erros } = parsearCSVEstoque(texto);
      setImportErros(erros);
      if (linhas.length === 0) {
        setImportResults([]);
        setModalImport(true);
        return;
      }
      const results: ImportResult[] = linhas.map((l) => ({
        nome: l.nome, status: 'pendente', msg: '',
      }));
      setImportResults(results);
      setModalImport(true);
      setImportando(true);

      const todosOsProdutos = await produtosService.listar();

      for (let i = 0; i < linhas.length; i++) {
        const linha = linhas[i];
        try {
          const encontrado = todosOsProdutos.find(
            (p) => p.nome.toLowerCase().trim() === linha.nome.toLowerCase().trim()
          );

          if (encontrado) {
            await produtosService.entradaEstoque(encontrado.id, linha.quantidade);
            results[i] = { nome: linha.nome, status: 'ok', msg: `+${linha.quantidade} unid. (estoque atualizado)` };
          } else if (linha.preco > 0 && linha.categoria) {
            await produtosService.criar({
              nome: linha.nome,
              preco: String(linha.preco),
              quantidadeEstoque: String(linha.quantidade),
              categoria: linha.categoria,
            });
            results[i] = { nome: linha.nome, status: 'ok', msg: 'Produto criado com sucesso' };
          } else {
            results[i] = { nome: linha.nome, status: 'erro', msg: 'Produto não encontrado. Para criar, informe: nome;preco;estoque;categoria' };
          }
        } catch {
          results[i] = { nome: linha.nome, status: 'erro', msg: 'Erro ao processar' };
        }
        setImportResults([...results]);
      }

      setImportando(false);
      qc.invalidateQueries({ queryKey: ['produtos'] });
    };
    reader.readAsText(file, 'UTF-8');
    e.target.value = '';
  }

  const okCount = importResults.filter((r) => r.status === 'ok').length;
  const erroCount = importResults.filter((r) => r.status === 'erro').length;

  return (
    <div className="space-y-5">
      {/* Toolbar */}
      <div className="flex flex-wrap gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={gerarModeloCSV}>
          <FileText size={14} /> Baixar Modelo CSV
        </Button>
        <Button variant="ghost" size="sm" onClick={handleExportCSV}>
          <Download size={14} /> Exportar CSV
        </Button>
        <Button variant="ghost" size="sm" onClick={handleExportPDF}>
          <FileDown size={14} /> Exportar PDF
        </Button>
        <Button size="sm" onClick={() => fileInputRef.current?.click()}>
          <Upload size={14} /> Importar CSV
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,.txt"
          className="hidden"
          onChange={handleFileChange}
        />
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Total de Produtos"
          value={String(total)}
          icon={<Package className="text-primary" size={22} />}
          iconBg="bg-primary-50"
        />
        <StatCard
          title="Estoque Zerado"
          value={String(zerados)}
          icon={<XCircle className="text-danger" size={22} />}
          iconBg="bg-danger-light"
        />
        <StatCard
          title="Crítico (≤ 3)"
          value={String(criticos)}
          icon={<AlertTriangle className="text-warning" size={22} />}
          iconBg="bg-warning-light"
        />
        <StatCard
          title="OK (> 10)"
          value={String(total - zerados - criticos - baixos)}
          icon={<CheckCircle className="text-success" size={22} />}
          iconBg="bg-success-light"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-5 py-3.5 border-b border-gray-100 flex items-center justify-between">
          <h3 className="font-semibold text-gray-800 text-sm">Inventário Completo</h3>
          <span className="text-xs text-gray-400">{total} produto{total !== 1 ? 's' : ''}</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                <th className="px-4 py-3 text-left font-medium">ID</th>
                <th className="px-4 py-3 text-left font-medium">Nome</th>
                <th className="px-4 py-3 text-left font-medium">Categoria</th>
                <th className="px-4 py-3 text-right font-medium">Preço</th>
                <th className="px-4 py-3 text-center font-medium">Estoque</th>
                <th className="px-4 py-3 text-center font-medium">Situação</th>
                <th className="px-4 py-3 text-center font-medium">Ajustar</th>
              </tr>
            </thead>
            <tbody>
              {produtos?.map((p, i) => {
                const { label, color, rowClass } = getEstoqueBadge(p.quantidadeEstoque);
                const highlight = p.quantidadeEstoque <= 10;
                return (
                  <tr
                    key={p.id}
                    className={`border-t border-gray-50 transition-colors ${
                      highlight
                        ? rowClass.split(' ')[0] + '/30'
                        : i % 2 === 0 ? 'bg-white' : 'bg-gray-50/40'
                    }`}
                  >
                    <td className="px-4 py-3 text-gray-400">#{p.id}</td>
                    <td className={`px-4 py-3 font-medium ${highlight ? rowClass.split(' ')[1] : 'text-gray-800'}`}>
                      {p.nome}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs bg-purple-50 text-primary rounded px-2 py-0.5">{p.categoria}</span>
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-700">
                      {formatCurrency(p.preco)}
                    </td>
                    <td className={`px-4 py-3 text-center font-bold text-lg ${highlight ? rowClass.split(' ')[1] : 'text-gray-700'}`}>
                      {p.quantidadeEstoque}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Badge color={color}>{label}</Badge>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => openAjuste(p)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary-50 transition-colors"
                        title="Ajustar estoque"
                      >
                        <Edit3 size={15} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Ajuste Modal */}
      <Modal
        open={modalAjuste}
        onClose={() => setModalAjuste(false)}
        title="Ajustar Estoque"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModalAjuste(false)}>Cancelar</Button>
            <Button
              onClick={() => {
                if (selected) ajusteMutation.mutate({ id: selected.id, qty: Number(novaQty) });
              }}
              loading={ajusteMutation.isPending}
            >
              Salvar
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <p className="text-sm font-medium text-gray-800">{selected?.nome}</p>
          <p className="text-sm text-gray-500">Estoque atual: <strong>{selected?.quantidadeEstoque}</strong></p>
          <div>
            <label className="form-label">Novo valor de estoque (absoluto)</label>
            <input
              type="number"
              min="0"
              value={novaQty}
              onChange={(e) => setNovaQty(e.target.value)}
              className="form-input"
              autoFocus
            />
          </div>
          {ajusteMutation.isError && (
            <p className="text-xs text-danger">{ajusteMutation.error?.message}</p>
          )}
        </div>
      </Modal>

      {/* Import Modal */}
      <Modal
        open={modalImport}
        onClose={() => !importando && setModalImport(false)}
        title="Importar Estoque via CSV"
        size="md"
        footer={
          <Button
            variant="ghost"
            onClick={() => setModalImport(false)}
            disabled={importando}
          >
            {importando ? 'Processando...' : 'Fechar'}
          </Button>
        }
      >
        <div className="space-y-4">
          {importErros.length > 0 && (
            <div className="bg-danger-light border border-red-200 rounded-lg p-3">
              <p className="text-xs font-semibold text-danger mb-1">Erros de formato:</p>
              {importErros.map((e, i) => <p key={i} className="text-xs text-danger">{e}</p>)}
            </div>
          )}

          {importResults.length > 0 && (
            <>
              <div className="flex gap-3 text-sm">
                <span className="text-success font-medium">{okCount} ok</span>
                <span className="text-danger font-medium">{erroCount} erro{erroCount !== 1 ? 's' : ''}</span>
                {importando && <span className="text-gray-400">processando...</span>}
              </div>
              <div className="border border-gray-100 rounded-lg overflow-hidden max-h-64 overflow-y-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50 text-gray-500 uppercase">
                      <th className="px-3 py-2 text-left font-medium">Produto</th>
                      <th className="px-3 py-2 text-left font-medium">Resultado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {importResults.map((r, i) => (
                      <tr key={i} className={`border-t border-gray-50 ${
                        r.status === 'ok' ? 'bg-success-light/40' :
                        r.status === 'erro' ? 'bg-danger-light/40' : 'bg-white'
                      }`}>
                        <td className="px-3 py-2 font-medium">{r.nome}</td>
                        <td className="px-3 py-2 text-gray-500">
                          {r.status === 'pendente' ? '⏳ aguardando...' :
                           r.status === 'ok' ? `✅ ${r.msg}` : `❌ ${r.msg}`}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          {importResults.length === 0 && importErros.length === 0 && (
            <div className="text-center py-8 text-gray-400">
              <p className="text-sm">Arquivo CSV vazio ou sem dados válidos.</p>
            </div>
          )}

          <div className="bg-purple-50 border border-purple-100 rounded-lg p-3 text-xs text-primary space-y-1">
            <p className="font-semibold">Formato esperado (separador: ponto e vírgula):</p>
            <code className="block bg-white border border-purple-100 rounded p-2 text-gray-700">
              nome;preco;estoque;categoria<br />
              Ração Premium;89.90;50;Ração
            </code>
            <p className="text-gray-500 mt-1">
              Se o produto já existir, o estoque é somado. Se não existir e tiver preço e categoria, é criado.
            </p>
          </div>
        </div>
      </Modal>
    </div>
  );
}
