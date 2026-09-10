package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import java.util.UUID;

public record Usuario(UUID id, String usuario, String nombre, Rol rol) {}
