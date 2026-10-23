# Atualizações executadas no WebMarket

O arquivo `webmarket.md` foi atualizado para adotar Spring Boot como framework das aplicações `ms-pedido` e `ms-produto`.

## Alterações realizadas

- Atualizada a identificação da arquitetura e das duas aplicações para Spring Boot.
- Atualizadas as descrições dos módulos Maven e dos artefatos executáveis.
- Substituídos os comandos de desenvolvimento por `spring-boot:run`.
- Atualizadas as configurações para propriedades Spring Boot, incluindo nome da aplicação, porta HTTP e conexão H2.
- Substituídas as dependências de persistência específicas do framework anterior por `spring-boot-starter-data-jpa` e `h2`.
- Atualizadas as propriedades JPA/Hibernate para desenvolvimento e para o perfil de testes com H2 em memória.
- Atualizado o exemplo de verificação Pact para usar `@SpringBootTest`, porta aleatória e `@LocalServerPort`.
- Atualizados os itens de implementação dos serviços para Spring Boot.

A estrutura de microsserviços, as responsabilidades de negócio e os demais componentes arquiteturais descritos no documento foram preservados.
