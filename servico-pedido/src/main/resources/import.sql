INSERT INTO Pedido (id, cliente_nome, data_criacao, status, valor_total) VALUES (1, 'Maria Silva', CURRENT_TIMESTAMP, 'CONFIRMADO', 370.00);
INSERT INTO ItemPedido (id, pedido_id, produto_id, quantidade, preco_unitario) VALUES (1, 1, 1, 1, 250.00);
INSERT INTO ItemPedido (id, pedido_id, produto_id, quantidade, preco_unitario) VALUES (2, 1, 2, 1, 120.00);

INSERT INTO Pedido (id, cliente_nome, data_criacao, status, valor_total) VALUES (2, 'João Santos', CURRENT_TIMESTAMP, 'CRIADO', 250.00);
INSERT INTO ItemPedido (id, pedido_id, produto_id, quantidade, preco_unitario) VALUES (3, 2, 1, 1, 250.00);