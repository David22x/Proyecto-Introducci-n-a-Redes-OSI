package OSI;

import java.util.Scanner;

/**
 * Punto de entrada del programa.
 * Valida el mensaje (250–500 caracteres) y orquesta el flujo OSI completo.
 */
public class Main {

    private static final int MIN_CARACTERES = 250;
    private static final int MAX_CARACTERES = 500;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Emisor emisor = new Emisor();
        Receptor receptor = new Receptor();

        Visualizador.mostrarEncabezado();

        boolean continuar = true;

        while (continuar) {
            Visualizador.mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {

                case "1":
                    System.out.println();
                    System.out.printf(
                            "Ingresa el mensaje a enviar (%d-%d caracteres): ",
                            MIN_CARACTERES, MAX_CARACTERES);
                    String mensaje = scanner.nextLine();

                    // ── Validación de longitud ──────────────────────────────
                    if (mensaje.trim().isEmpty()) {
                        Visualizador.mostrarErrorMensajeVacio();
                        break;
                    }

                    int longitud = mensaje.length();
                    if (longitud < MIN_CARACTERES || longitud > MAX_CARACTERES) {
                        Visualizador.mostrarErrorLongitud(longitud, MIN_CARACTERES, MAX_CARACTERES);
                        break;
                    }

                    // ── ENVÍO: encapsulación capa 7 → capa 1 ───────────────
                    PDU pduTransmitida = emisor.enviar(mensaje);

                    // ── RECEPCIÓN: desencapsulación capa 1 → capa 7 ────────
                    String mensajeRecuperado = receptor.recibir(pduTransmitida);

                    // ── RESULTADO FINAL ─────────────────────────────────────
                    Visualizador.mostrarResultadoFinal(mensaje, mensajeRecuperado);
                    break;

                case "2":
                    continuar = false;
                    Visualizador.mostrarDespedida();
                    break;

                default:
                    System.out.println("  ✗ Opcion invalida. Elige 1 o 2.\n");
            }
        }

        scanner.close();
    }
}