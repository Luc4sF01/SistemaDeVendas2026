package repository;

import model.Venda;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendaRepositorio {
    private final List<Venda> vendas = new ArrayList<>();
    private int proximoId = 1;

    public Venda salvar(Venda venda) {
        vendas.add(venda);
        return venda;
    }

    public int gerarId() {
        return proximoId++;
    }

    public Optional<Venda> buscarPorId(int id) {
        return vendas.stream().filter(v -> v.getId() == id).findFirst();
    }

    public List<Venda> listarTodas() {
        return new ArrayList<>(vendas);
    }

    public List<Venda> listarPorCliente(int clienteId) {
        return vendas.stream()
                .filter(v -> v.getCliente() != null && v.getCliente().getId() == clienteId)
                .toList();
    }
}
