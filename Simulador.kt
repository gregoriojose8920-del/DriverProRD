fun main() {
    val minPrice = 100.0
    val maxDistance = 5.0
    println("--- Simulación del Bot MGO Driver Pro ---")
    val viajePrecio = 120.0
    val viajeDistancia = 3.0
    println("Viaje: $viajePrecio por $viajeDistancia km")
    if (viajePrecio >= minPrice && viajeDistancia <= maxDistance) {
        println("ESTADO: VIAJE ACEPTADO ✅")
    } else {
        println("ESTADO: VIAJE RECHAZADO ❌")
    }
}
