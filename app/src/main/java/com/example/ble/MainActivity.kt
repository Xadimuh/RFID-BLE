package com.example.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var textViewMessages: TextView
    private lateinit var btnClear: Button
    private lateinit var btnOpen: Button
    private lateinit var btnClose: Button
    private var bluetoothGatt: BluetoothGatt? = null
    private val deviceAddress = "00:15:85:14:9C:09" // Adresse MAC du périphérique CC2541

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        textViewMessages = findViewById(R.id.textViewMessages)
        btnClear = findViewById(R.id.btnClear)
        btnOpen = findViewById(R.id.btnOpen)
        btnClose = findViewById(R.id.btnClose)

        // Vérifier et demander les permissions Bluetooth
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1)
            return
        }

        // Se connecter au Bluetooth
        connectToBluetooth()

        // Écouteur pour le bouton "Effacer"
        btnClear.setOnClickListener {
            textViewMessages.text = "" // Effacer les messages dans la TextView
        }

        // Écouteur pour le bouton "Ouvrir"
        btnOpen.setOnClickListener {
            sendBluetoothCommand('O') // Envoie la commande 'O' pour ouvrir la porte
        }

        // Écouteur pour le bouton "Fermer"
        btnClose.setOnClickListener {
            sendBluetoothCommand('F') // Envoie la commande 'F' pour fermer la porte
        }
    }

    private fun connectToBluetooth() {
        // Vérifier si Bluetooth est activé
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("Bluetooth", "Bluetooth non disponible ou désactivé")
            return
        }

        // Obtenir le périphérique Bluetooth avec l'adresse MAC
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)

        device?.let {
            // Se connecter au périphérique Bluetooth Low Energy
            bluetoothGatt = it.connectGatt(this, false, bluetoothGattCallback)
        }
    }

    private fun sendBluetoothCommand(command: Char) {
        bluetoothGatt?.let { gatt ->
            // Créer une caractéristique pour envoyer la commande
            val serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
            val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)

            characteristic?.let {
                it.setValue(command.toString())
                gatt.writeCharacteristic(it) // Envoie la commande à l'Arduino
                Log.d("Bluetooth", "Commande envoyée : $command")
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("Bluetooth", "Connecté au périphérique BLE")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d("Bluetooth", "Déconnecté du périphérique BLE")
                runOnUiThread {
                    textViewMessages.append("\nDéconnecté du périphérique")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Bluetooth", "Services découverts")
                gatt.services.forEach { service ->
                    Log.d("Bluetooth", "Service: ${service.uuid}")
                }
            } else {
                Log.e("Bluetooth", "Erreur lors de la découverte des services")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                val message = String(data)  // Convertir les données en chaîne
                Log.d("Bluetooth", "Message reçu : $message")
                runOnUiThread {
                    appendMessage(message)  // Afficher le message dans la TextView
                }
            }
        }

        // Gérer les notifications de caractéristiques
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val data = characteristic.value
            val message = String(data)  // Convertir les données en chaîne
            Log.d("Bluetooth", "Notification reçue : $message")
            runOnUiThread {
                appendMessage(message)  // Afficher le message dans la TextView
            }
        }
    }

    private fun appendMessage(message: String) {
        textViewMessages.append("\n$message")  // Ajouter le message à la TextView
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close() // Fermer la connexion Bluetooth
    }
}
