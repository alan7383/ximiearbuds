package com.alan.ximiearbuds.core.bluetooth

object TransportFactory {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val isLinux = System.getProperty("os.name").lowercase().contains("linux")

    fun createTransport(useSimulation: Boolean = false): BluetoothTransport {
        if (useSimulation) {
            return SimulatedTransport()
        }

        return when {
            isWindows -> WindowsBthTransport()
            isLinux -> LinuxRfcommTransport()
            else -> SimulatedTransport()
        }
    }
}
