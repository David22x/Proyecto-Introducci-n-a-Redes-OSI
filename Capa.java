package OSI;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.Base64;

/**
 * Interfaz comun para todas las capas del modelo OSI.
 * Define el contrato de encapsulacion y desencapsulacion.
 */
public interface Capa {

    PDU encapsular(PDU pdu);

    /**
     * Implementacion por defecto: elimina la cabecera separada por " | ".
     * Las capas con logica especial sobreescriben este metodo.
     */
    default PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int separador = contenido.indexOf(" | ");
        if (separador != -1) {
            contenido = contenido.substring(separador + 3);
        }
        return new PDU(contenido);
    }

    String getNombre();
}

// ============================================================
// CAPA 7 - Aplicacion
//
// FUNCION: Define las reglas que permiten la comunicacion.
// Establece el protocolo que usaran emisor y receptor para
// entenderse (HTTP, FTP, SMTP, etc.), especificando el
// metodo de la peticion, la version del protocolo, el
// recurso solicitado y el host destino.
// PDU resultante = MENSAJE
// ============================================================
class CapaAplicacion implements Capa {

    private static final String PROTOCOLO = "HTTP";
    private static final String VERSION = "1.1";
    private static final String METODO = "GET";
    private static final String RECURSO = "/index.html";
    private static final String HOST = "servidor.uta.edu.ec";

    @Override
    public PDU encapsular(PDU pdu) {
        String cabecera = String.format(
                "[AH: protocolo=%s/%s, metodo=%s, recurso=%s, host=%s, content-length=%d]",
                PROTOCOLO, VERSION, METODO, RECURSO, HOST, pdu.getDatos().length());
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    @Override
    public String getNombre() {
        return "Capa 7 - Aplicacion";
    }
}

// ============================================================
// CAPA 6 - Presentacion
//
// FUNCION: Reduce el tamano de los datos para optimizar
// su transmision (compresion).
// Esta capa transforma el formato de los datos aplicando
// compresion, lo que reduce la cantidad de bytes a transmitir
// y optimiza el uso del ancho de banda disponible.
//
// Se comprime con el algoritmo DEFLATE (el mismo que usa
// ZIP/GZIP) y se codifica en Base64 para representarlo como
// texto transmisible. La cabecera reporta el tamano original,
// el comprimido y el porcentaje de reduccion logrado.
// PDU resultante = DATO COMPRIMIDO
// ============================================================
class CapaPresentacion implements Capa {

    @Override
    public PDU encapsular(PDU pdu) {
        String datosOriginales = pdu.getDatos();
        int tamanoOriginal = datosOriginales.length();

        String comprimido = comprimir(datosOriginales);
        int tamanoComprimido = comprimido.length();

        int reduccion = (int) (((double) (tamanoOriginal - tamanoComprimido) / tamanoOriginal) * 100);
        String estado = reduccion > 0 ? "reduccion=" + reduccion + "%" : "sin-reduccion";

        String cabecera = String.format(
                "[PH: codificacion=UTF-8, compresion=DEFLATE, tam_original=%d, tam_comprimido=%d, %s]",
                tamanoOriginal, tamanoComprimido, estado);
        return new PDU(cabecera + " | " + comprimido);
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int sep = contenido.indexOf(" | ");
        if (sep != -1) {
            contenido = contenido.substring(sep + 3);
        }
        return new PDU(descomprimir(contenido));
    }

    private String comprimir(String texto) {
        try {
            byte[] input = texto.getBytes("UTF-8");
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(input);
            deflater.finish();
            byte[] output = new byte[input.length * 2];
            int len = deflater.deflate(output);
            deflater.end();
            byte[] resultado = new byte[len];
            System.arraycopy(output, 0, resultado, 0, len);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            return texto;
        }
    }

    private String descomprimir(String comprimido) {
        try {
            byte[] input = Base64.getDecoder().decode(comprimido);
            Inflater inflater = new Inflater();
            inflater.setInput(input);
            byte[] output = new byte[input.length * 10];
            int len = inflater.inflate(output);
            inflater.end();
            return new String(output, 0, len, "UTF-8");
        } catch (Exception e) {
            return comprimido;
        }
    }

    @Override
    public String getNombre() {
        return "Capa 6 - Presentacion";
    }
}

// ============================================================
// CAPA 5 - Sesion
//
// FUNCION: Controla como se lleva a cabo la comunicacion.
// Gestiona el dialogo entre los sistemas: abre la sesion,
// la mantiene activa mediante checkpoints (puntos de control
// que permiten reanudar si se interrumpe) y define el modo
// de comunicacion (simplex: un solo sentido).
// PDU resultante = DATO CON CONTROL DE SESION
// ============================================================
class CapaSesion implements Capa {

    private static int contadorSesiones = 0;
    private final String sesionId;
    private final String horaInicio;
    private int checkpoint = 0;

    public CapaSesion() {
        contadorSesiones++;
        this.sesionId = String.format("SES-%03d", contadorSesiones);
        this.horaInicio = new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new java.util.Date());
    }

    @Override
    public PDU encapsular(PDU pdu) {
        checkpoint++;
        String cabecera = String.format(
                "[SH: sesion_id=%s, inicio=%s, modo=simplex, checkpoint=%d, estado=ABIERTA]",
                sesionId, horaInicio, checkpoint);
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    @Override
    public String getNombre() {
        return "Capa 5 - Sesion";
    }
}

// ============================================================
// CAPA 4 - Transporte
//
// FUNCION: Regula la cantidad de datos que se transmiten
// (control de flujo).
// TCP usa una ventana deslizante que limita cuantos bytes
// puede enviar el emisor antes de recibir confirmacion del
// receptor, evitando que lo sature con mas datos de los que
// puede procesar en un momento dado.
//
// Se segmenta el mensaje en bloques del tamano de la ventana
// (TAMANIO_SEGMENTO). Cada segmento lleva numero de secuencia
// y el tamano de ventana disponible, igual que TCP real.
//
// SEPARADOR entre segmentos: "<<<SEG>>>" (nunca aparece en
// cabeceras ni en Base64, evita colisiones con el contenido).
// PDU resultante = SEGMENTOS CON CONTROL DE FLUJO
// ============================================================
class CapaTransporte implements Capa {

    public static final int TAMANIO_SEGMENTO = 50;
    // Separador unico que no puede aparecer en cabeceras ni Base64
    private static final String SEP = "<<<SEG>>>";

    @Override
    public PDU encapsular(PDU pdu) {
        String datos = pdu.getDatos();
        StringBuilder resultado = new StringBuilder();
        int totalSegmentos = (int) Math.ceil((double) datos.length() / TAMANIO_SEGMENTO);

        for (int i = 0; i < totalSegmentos; i++) {
            int inicio = i * TAMANIO_SEGMENTO;
            int fin = Math.min(inicio + TAMANIO_SEGMENTO, datos.length());
            String fragmento = datos.substring(inicio, fin);

            String cabecera = String.format(
                    "[TH: proto=TCP, src_port=1234, dst_port=80, seq=%04d, total=%04d, ventana=%d_bytes]",
                    i + 1, totalSegmentos, TAMANIO_SEGMENTO);
            resultado.append(cabecera).append(" | ").append(fragmento);
            if (i < totalSegmentos - 1)
                resultado.append(SEP);
        }

        return new PDU(resultado.toString());
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        // Dividimos por el separador y reensamblamos en orden de secuencia
        String[] segmentos = pdu.getDatos().split(SEP);
        StringBuilder reconstruido = new StringBuilder();
        for (String seg : segmentos) {
            int sep = seg.indexOf(" | ");
            if (sep != -1) {
                reconstruido.append(seg.substring(sep + 3));
            }
        }
        return new PDU(reconstruido.toString());
    }

    @Override
    public String getNombre() {
        return "Capa 4 - Transporte";
    }
}

// ============================================================
// CAPA 3 - Red
//
// FUNCION: Enrutar paquetes desde el origen hasta el destino.
// Determina el mejor camino a traves de multiples routers.
// Cada router decrementa el TTL (Time To Live) en 1; si
// llega a 0, el paquete se descarta para evitar que circule
// indefinidamente por la red formando bucles.
//
// Se simula una tabla de enrutamiento con saltos intermedios.
// Se calcula la ruta completa y el TTL resultante despues
// de decrementarlo en cada router atravesado.
// PDU resultante = PAQUETE ENRUTADO
// ============================================================
class CapaRed implements Capa {

    private static final String[][] TABLA_ENRUTAMIENTO = {
            { "200.10.5.3", "192.168.1.1", "10.0.0.1", "200.10.5.3" }
    };

    private static final String IP_ORIGEN = "192.168.1.10";
    private static final String IP_DESTINO = "200.10.5.3";
    private static final int TTL_INICIAL = 64;

    @Override
    public PDU encapsular(PDU pdu) {
        String ruta = calcularRuta(IP_DESTINO);
        int ttlFinal = calcularTTL(IP_DESTINO);

        String cabecera = String.format(
                "[NH: ip_origen=%s, ip_destino=%s, TTL=%d, proto=TCP, ruta=%s]",
                IP_ORIGEN, IP_DESTINO, ttlFinal, ruta);
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    private String calcularRuta(String destino) {
        for (String[] entrada : TABLA_ENRUTAMIENTO) {
            if (entrada[0].equals(destino)) {
                StringBuilder ruta = new StringBuilder(IP_ORIGEN);
                for (String salto : entrada) {
                    ruta.append("->").append(salto);
                }
                return ruta.toString();
            }
        }
        return IP_ORIGEN + "->" + destino;
    }

    private int calcularTTL(String destino) {
        for (String[] entrada : TABLA_ENRUTAMIENTO) {
            if (entrada[0].equals(destino)) {
                return TTL_INICIAL - (entrada.length - 1);
            }
        }
        return TTL_INICIAL - 1;
    }

    @Override
    public String getNombre() {
        return "Capa 3 - Red";
    }
}

// ============================================================
// CAPA 2 - Enlace de datos
//
// FUNCION: Garantizar la confiabilidad de la informacion.
// Detecta errores de transmision mediante CRC (Cyclic
// Redundancy Check). El emisor calcula el CRC y lo adjunta.
// El receptor lo recalcula: si coincide, la trama llego
// integra; si no coincide, fue corrompida y se solicitaria
// retransmision.
//
// Se calcula un CRC-16 real usando el polinomio estandar
// 0x8005 (el mismo que usan Ethernet y USB). Al desencapsular
// se verifica que el CRC recibido coincida con el recalculado.
// PDU resultante = TRAMA VERIFICADA
// ============================================================
class CapaEnlace implements Capa {

    @Override
    public PDU encapsular(PDU pdu) {
        int crc = calcularCRC16(pdu.getDatos());
        String cabecera = String.format(
                "[DH: mac_origen=AA:BB:CC:DD:EE:01, mac_destino=AA:BB:CC:DD:EE:02, CRC16=0x%04X, integridad=OK]",
                crc);
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int crcRecibido = extraerCRC(contenido);

        int sep = contenido.indexOf(" | ");
        String datos = (sep != -1) ? contenido.substring(sep + 3) : contenido;

        int crcCalculado = calcularCRC16(datos);
        if (crcRecibido != -1 && crcRecibido != crcCalculado) {
            System.out.println("  ADVERTENCIA: CRC no coincide. Trama posiblemente corrompida.");
        }

        return new PDU(datos);
    }

    public int calcularCRC16(String datos) {
        int crc = 0xFFFF;
        for (char c : datos.toCharArray()) {
            crc ^= (c << 8);
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? (crc << 1) ^ 0x8005 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    private int extraerCRC(String cabecera) {
        try {
            int idx = cabecera.indexOf("CRC16=0x");
            if (idx == -1)
                return -1;
            return Integer.parseInt(cabecera.substring(idx + 8, idx + 12), 16);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String getNombre() {
        return "Capa 2 - Enlace de datos";
    }
}

// ============================================================
// CAPA 1 - Fisica
//
// FUNCION: Convertir los datos a bits para transmitirlos
// por el medio fisico (cable, fibra optica, aire).
// Esta capa no entiende de bytes ni caracteres: solo trabaja
// con ceros y unos que representan senales electricas,
// opticas o electromagneticas en el medio de transmision.
//
// Se convierte cada byte del mensaje a su representacion
// binaria de 8 bits. La cabecera reporta el total de bits
// generados, la velocidad simulada del medio y la
// codificacion de linea NRZ (estandar en Ethernet).
// PDU resultante = FLUJO DE BITS
// ============================================================
class CapaFisica implements Capa {

    private static final int VELOCIDAD_MBPS = 100;

    @Override
    public PDU encapsular(PDU pdu) {
        StringBuilder bits = new StringBuilder();
        byte[] bytes;
        try {
            bytes = pdu.getDatos().getBytes("UTF-8");
        } catch (Exception e) {
            bytes = pdu.getDatos().getBytes();
        }

        for (byte b : bytes) {
            bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0'));
            bits.append(" ");
        }

        int totalBits = bytes.length * 8;
        String cabecera = String.format(
                "[FISICO: total_bits=%d, velocidad=%dMbps, codificacion=NRZ]",
                totalBits, VELOCIDAD_MBPS);

        return new PDU(cabecera + " | [BITS: " + bits.toString().trim() + "]");
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();

        int sep = contenido.indexOf(" | ");
        if (sep != -1) {
            contenido = contenido.substring(sep + 3);
        }

        if (contenido.startsWith("[BITS: ") && contenido.endsWith("]")) {
            contenido = contenido.substring(7, contenido.length() - 1).trim();
            String[] grupos = contenido.split(" ");
            StringBuilder texto = new StringBuilder();
            for (String grupo : grupos) {
                if (!grupo.isEmpty()) {
                    texto.append((char) Integer.parseInt(grupo, 2));
                }
            }
            return new PDU(texto.toString());
        }
        return pdu;
    }

    @Override
    public String getNombre() {
        return "Capa 1 - Fisica";
    }
}
