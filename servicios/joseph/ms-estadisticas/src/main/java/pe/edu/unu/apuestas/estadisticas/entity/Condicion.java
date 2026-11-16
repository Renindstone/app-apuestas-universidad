package pe.edu.unu.apuestas.estadisticas.entity;

/**
 * Si el equipo jugó en casa o fuera. TODOS solo se usa como filtro de los promedios: ninguna fila se guarda con TODOS.
 */
public enum Condicion {
    LOCAL, VISITANTE, TODOS
}
