package br.com.vibetex.infrastructure.clients.controlapp;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControlAppUsuarioDTO {

    @JsonAlias({ "usuarioId", "UsuarioId", "id", "Id" })
    private UUID usuarioId;

    @JsonAlias({ "nome", "Nome", "name" })
    private String nome;
    
    @JsonProperty("Email")
    private String email;


    @JsonAlias({ "nomeDaEmpresa", "NomeDaEmpresa", "nomeEmpresa", "NomeEmpresa" })
    private String nomeDaEmpresa;

    @JsonAlias({ "empresaId", "EmpresaId" })
    private UUID empresaId;
}
