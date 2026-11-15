package br.gov.caixa.webmarket.produto.domain.entity;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.*;

@Entity
@Table(name = "Usuario")
@UserDefinition
public class Usuario {

    @Id
    @SequenceGenerator(name = "usuarioSeq", sequenceName = "usuario_id_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuarioSeq")
    private Long id;

    @Username
    @Column(nullable = false, unique = true)
    private String username;

    @Password
    @Column(nullable = false)
    private String senha;

    @Roles
    @Column(nullable = false)
    private String papeis;

    public Usuario() {
    }

    public Usuario(String username, String senhaHash, String papeis) {
        this.username = username;
        this.senha = senhaHash;
        this.papeis = papeis;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getSenha() { return senha; }
    public String getPapeis() { return papeis; }
}