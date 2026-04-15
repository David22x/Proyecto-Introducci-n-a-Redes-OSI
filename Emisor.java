package OSI;

import java.util.ArrayList;
import java.util.List;

/**
 * Emisor: recibe datos del usuario y los encapsula
 * recorriendo las capas OSI de arriba (capa 7) hacia abajo (capa 1).
 *
 * La lógica de visualización se delega al Visualizador,
 * manteniendo la separación de responsabilidades.
 */
public class Emisor {

    private final List<Capa> capas = new ArrayList<>();

    // Descripciones del proceso realizado en cada capa (para visualización)
    private static final String[] PROCESOS_ENCAPSULACION = {
            "Agrega cabecera HTTP con protocolo y version",
            "Cifra contenido con Cesar-3, agrega cabecera de codificacion",
            "Asigna identificador de sesion unico",
            "Segmenta en bloques de " + CapaTransporte.TAMANIO_SEGMENTO + " chars y numera cada segmento",
            "Agrega IPs de origen/destino y TTL (enrutamiento logico)",
            "Agrega MACs de origen/destino y CRC calculado",
            "Convierte cada caracter a 8 bits binarios"
    };

    public Emisor() {
        capas.add(new CapaAplicacion());
        capas.add(new CapaPresentacion());
        capas.add(new CapaSesion());
        capas.add(new CapaTransporte());
        capas.add(new CapaRed());
        capas.add(new CapaEnlace());
        capas.add(new CapaFisica());
    }

    /**
     * Encapsula el mensaje pasando por cada capa de arriba a abajo.
     *
     * @param mensaje El texto ingresado por el usuario
     * @return PDU final en forma de bits, lista para "transmitir"
     */
    public PDU enviar(String mensaje) {
        Visualizador.mostrarInicioEnvio(mensaje);

        PDU pdu = new PDU(mensaje);

        for (int i = 0; i < capas.size(); i++) {
            Capa capa = capas.get(i);
            String entradaAntes = pdu.getDatos();
            pdu = capa.encapsular(pdu);
            Visualizador.mostrarPasoEncapsulacion(
                    capa.getNombre(),
                    entradaAntes,
                    pdu.getDatos(),
                    PROCESOS_ENCAPSULACION[i]);
        }

        return pdu;
    }
}