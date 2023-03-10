package com.example.grpc_chess

data class ChessPiece(val col: Int, val row: Int, val player: Player, val chessman: Chessman, val resID: Int) {
}