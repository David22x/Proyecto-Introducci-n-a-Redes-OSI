package OSI;

/**
 * Visualizador: responsable de toda la salida por consola.
 * Separa la lógica de visualización del modelo OSI,
 * cumpliendo el requisito de separación de responsabilidades.
 */
public class Visualizador {

    private static final int ANCHO_CONSOLA = 70;

    public static void mostrarEncabezado() {
        linea('=');
        centrar("Simulador Modelo OSI - Bidireccional");
        centrar("Capas: Fisica | Enlace | Red | Transporte | Sesion | Presentacion | Aplicacion");
        linea('=');
    }

    public static void mostrarMenu() {
        System.out.println();
        linea('-');
        centrar("MENU PRINCIPAL");
        linea('-');
        System.out.println("  1. Enviar un mensaje (encapsular + transmitir)");
        System.out.println("  2. Salir");
        linea('-');
        System.out.print("  Elige una opcion: ");
    }

    public static void mostrarInicioEnvio(String mensajeOriginal) {
        System.out.println();
        linea('=');
        centrar("PROCESO DE ENVIO  (Capa 7 a Capa 1)");
        linea('=');
        System.out.println();
        System.out.println("  Datos originales del usuario:");
        System.out.println("  |= Entrada ===================================================");
        System.out.println("  │  " + mensajeOriginal);
        System.out.println("  ==============================================================");
        System.out.println();
    }

    public static void mostrarPasoEncapsulacion(String nombreCapa, String entrada, String salida, String proceso) {
        System.out.println("  |= " + nombreCapa + " ===========================================");
        System.out.println("  │  Entrada  : " + truncar(entrada, 55));
        System.out.println("  │  Proceso  : " + proceso);
        System.out.println("  │  Salida   : " + truncar(salida, 55));
        System.out.println("  ==============================================================");
        System.out.println();
    }

    public static void mostrarInicioRecepcion(String bitsRecibidos) {
        linea('─');
        System.out.println();
        linea('=');
        centrar("PROCESO DE RECEPCION  (Capa 1  Capa 7)");
        linea('=');
        System.out.println();
        System.out.println("  Bits recibidos del medio fisico:");
        System.out.println("  " + truncar(bitsRecibidos, 65) + "...");
        System.out.println();
    }

    public static void mostrarPasoDesencapsulacion(String nombreCapa, String entrada, String salida, String proceso) {
        System.out.println("  |=" + nombreCapa + " ===========================================");
        System.out.println("  │  Entrada  : " + truncar(entrada, 55));
        System.out.println("  │  Proceso  : " + proceso);
        System.out.println("  │  Salida   : " + truncar(salida, 55));
        System.out.println("  ==============================================================");
        System.out.println();
    }

    public static void mostrarResultadoFinal(String original, String recuperado) {
        System.out.println();
        linea('=');
        centrar("RESULTADO FINAL");
        linea('=');
        System.out.println();
        System.out.printf("  Mensaje original enviado  : [%s]%n", original);
        System.out.printf("  Mensaje recuperado        : [%s]%n", recuperado);
        System.out.println();
        if (original.equals(recuperado)) {
            System.out.println("   El mensaje se transmitio correctamente.");
        } else {
            System.out.println("   Error: los mensajes no coinciden.");
        }
        System.out.println();
    }

    public static void mostrarErrorMensajeVacio() {
        System.out.println("   El mensaje no puede estar vacio.\n");
    }

    public static void mostrarErrorLongitud(int len, int min, int max) {
        System.out.printf(
                "   El mensaje tiene %d caracteres. Debe tener entre %d y %d caracteres.%n%n",
                len, min, max);
    }

    public static void mostrarDespedida() {
        System.out.println("\n¡Hasta luego!\n");
    }

    // ─── Utilidades 

    private static String truncar(String texto, int max) {
        if (texto == null)
            return "";
        return texto.length() > max ? texto.substring(0, max) + "..." : texto;
    }

    private static void linea(char caracter) {
        System.out.println(String.valueOf(caracter).repeat(ANCHO_CONSOLA));
    }

    private static void centrar(String texto) {
        int padding = Math.max(0, (ANCHO_CONSOLA - texto.length()) / 2);
        System.out.println(" ".repeat(padding) + texto);
    }
}
