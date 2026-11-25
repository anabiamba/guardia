package br.com.guardia.util;

public class LocationUtils {

    private static final double RAIO_TERRA_METROS = 6371000.0;

    public static double calcularDistanciaEmMetros(
            double latitudeInicial,
            double longitudeInicial,
            double latitudeFinal,
            double longitudeFinal
    ) {

        double lat1 = Math.toRadians(latitudeInicial);
        double lat2 = Math.toRadians(latitudeFinal);

        double diferencaLatitude =
                Math.toRadians(latitudeFinal - latitudeInicial);

        double diferencaLongitude =
                Math.toRadians(longitudeFinal - longitudeInicial);

        double a =
                Math.sin(diferencaLatitude / 2)
                        * Math.sin(diferencaLatitude / 2)
                + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(diferencaLongitude / 2)
                        * Math.sin(diferencaLongitude / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return RAIO_TERRA_METROS * c;
    }
}