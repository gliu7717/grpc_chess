package com.example.grpc_chess

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import chessPackage.ChessGrpc
import chessPackage.ChessGrpc.ChessBlockingStub
import chessPackage.ChessOuterClass
import chessPackage.ChessOuterClass.ChessMove
import com.example.grpc_chess.R
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ChessDelegate {
    private var isReset: Boolean = false
    private var channel: ManagedChannel? = null
    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var listenButton: Button
    private lateinit var connectButton: Button
    private val isEmulator = Build.FINGERPRINT.contains("generic")
    private lateinit var blockingStub: ChessBlockingStub
    private var step:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InitGrpc()
        setContentView(R.layout.activity_main)

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)
        chessView.chessDelegate = this
        resetButton.setOnClickListener {
            ChessGame.reset()
            chessView.invalidate()
            listenButton.isEnabled = true
            isReset = true;
        }

        listenButton.setOnClickListener {
            listenButton.isEnabled = false
            val port= 8080
            Toast.makeText(this, "listening on $port", Toast.LENGTH_SHORT).show()
            val blockingStub = ChessGrpc.newBlockingStub(channel)
            try {
                Executors.newSingleThreadExecutor().execute {
                    while( !isReset ) {
                        var move: ChessMove
                        val receveRequest = ChessOuterClass.noparam.newBuilder().build()
                        move = blockingStub.receiveChessMove(receveRequest)
                        Log.i("grpc", "Gotmove string" + move.toString())
                        Log.i("grpc", "Waiting for step:" + step.toString())

                        if(move.step == step + 1)
                            receiveMove(move)
                        Thread.sleep(1_000)
                    }
                }
            } catch (e: Exception) {
                Log.i("grpc", (e.message?:""))
            }
        }

    }

    private fun InitGrpc(){
        Log.i("grpc", "Start to test grpc")
        try {
            //val host: String = "192.9.133.7"
            //val portStr: String = "40000"
            val host: String = "150.136.175.102"
            val portStr: String = "8080"
            val port = if (portStr.isEmpty()) 0 else Integer.valueOf(portStr)
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
            Log.i("grpc", "Started grpc on " + host + ":" + portStr)
            blockingStub = ChessGrpc.newBlockingStub(channel)
        } catch (e: Exception) {
            Log.i("grpc", (e.message?: ""))
        }
    }

    private fun test(){
        Log.i("grpc", "Start to test grpc")
        try {
            //val host: String = "192.9.133.7"
            //val portStr: String = "40000"
            val host: String = "150.136.175.102"
            val portStr: String = "8080"
            val port = if (portStr.isEmpty()) 0 else Integer.valueOf(portStr)
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
            Log.i("grpc", "Started grpc on " + host + ":" + portStr)
            val sendRequest = ChessOuterClass.ChessMove.newBuilder().setStep(1).setFromX(1).
            setFromY(2).setToX(3).setToY(4).build()

            val receveRequest = ChessOuterClass.noparam.newBuilder().build()
            val cheseMove:ChessMove
            val rcheseMove:ChessMove
            val blockingStub = ChessGrpc.newBlockingStub(channel)
            try {
                cheseMove  = blockingStub.sendChessMove(sendRequest)
                Log.i("grpc", cheseMove.toString())
                rcheseMove  = blockingStub.receiveChessMove(receveRequest)
                Log.i("grpc", rcheseMove.toString())
            } catch (e: Exception) {
                Log.i("grpc", (e.message?:""))
            }

        } catch (e: Exception) {
            Log.i("grpc", (e.message?: ""))
        }
    }

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    override fun movePiece(from: Square, to: Square) {
        ChessGame.movePiece(from, to)
        chessView.invalidate()
        val moveStr = "movstring : ${from.col},${from.row},${to.col},${to.row}"
        Log.i("grpc", moveStr)
        try {
            step++
            Log.i("grpc", " generated step:" + step.toString())

            val sendRequest = ChessOuterClass.ChessMove.newBuilder().
            setStep(step).
            setFromX(from.col).
            setFromY(from.row).
            setToX(to.col).
            setToY(to.row).build()
            val cheseMove:ChessMove
            try {
                cheseMove  = blockingStub.sendChessMove(sendRequest)
                Log.i("grpc", cheseMove.toString())
            } catch (e: Exception) {
                Log.i("grpc", (e.message?:""))
            }

        } catch (e: Exception) {
            Log.i("grpc", (e.message?: ""))
        }
    }

    private fun receiveMove(move:ChessMove) {
        runOnUiThread{
            ChessGame.movePiece(Square(move.fromX, move.fromY), Square(move.toX, move.toY))
            chessView.invalidate()
        }
    }

}