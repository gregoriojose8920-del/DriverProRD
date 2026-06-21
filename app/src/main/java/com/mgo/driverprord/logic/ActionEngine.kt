package com.mgo.driverprord.logic

class ActionEngine {
    var minPrice: Double = 0.0
    var maxDistance: Double = 0.0
    var currency: String = "DOP" // Moneda por defecto
    
    fun shouldAccept(price: Double, distance: Double): Boolean {
        return price >= minPrice && distance <= maxDistance
    }
}
