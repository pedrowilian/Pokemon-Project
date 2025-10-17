package shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Classe utilitária para manipulação de datas
 * Implementa funcionalidades de formatação, cálculo e validação de datas
 * Requisito do projeto: Manipulação de Datas
 */
public class DateUtils {

    // Formatadores de data comuns
    public static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter BR_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Formata uma data no formato brasileiro (dd/MM/yyyy)
     * @param date Data a ser formatada
     * @return String formatada
     */
    public static String formatDateBR(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(BR_DATE_FORMAT);
    }

    /**
     * Formata uma data/hora no formato brasileiro (dd/MM/yyyy HH:mm:ss)
     * @param dateTime Data/hora a ser formatada
     * @return String formatada
     */
    public static String formatDateTimeBR(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(BR_DATETIME_FORMAT);
    }

    /**
     * Formata apenas a hora (HH:mm:ss)
     * @param dateTime Data/hora a ser formatada
     * @return String formatada
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMAT);
    }

    /**
     * Converte string no formato ISO para LocalDateTime
     * @param isoString String no formato ISO
     * @return LocalDateTime ou null se inválido
     */
    public static LocalDateTime parseISODateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoString, ISO_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converte string no formato brasileiro para LocalDate
     * @param brString String no formato dd/MM/yyyy
     * @return LocalDate ou null se inválido
     */
    public static LocalDate parseBRDate(String brString) {
        if (brString == null || brString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(brString, BR_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calcula a diferença em dias entre duas datas
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Número de dias entre as datas
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calcula a diferença em dias entre duas datas/horas
     * @param startDateTime Data/hora inicial
     * @param endDateTime Data/hora final
     * @return Número de dias entre as datas
     */
    public static long daysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDateTime, endDateTime);
    }

    /**
     * Calcula a diferença em horas entre duas datas/horas
     * @param startDateTime Data/hora inicial
     * @param endDateTime Data/hora final
     * @return Número de horas entre as datas
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    /**
     * Calcula a diferença em minutos entre duas datas/horas
     * @param startDateTime Data/hora inicial
     * @param endDateTime Data/hora final
     * @return Número de minutos entre as datas
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    /**
     * Adiciona dias a uma data
     * @param date Data base
     * @param days Número de dias a adicionar (pode ser negativo)
     * @return Nova data
     */
    public static LocalDate addDays(LocalDate date, long days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }

    /**
     * Adiciona dias a uma data/hora
     * @param dateTime Data/hora base
     * @param days Número de dias a adicionar (pode ser negativo)
     * @return Nova data/hora
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * Verifica se uma data expirou (passou da data atual)
     * @param date Data a verificar
     * @return true se a data já passou
     */
    public static boolean isExpired(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.isBefore(LocalDate.now());
    }

    /**
     * Verifica se uma data/hora expirou (passou da data/hora atual)
     * @param dateTime Data/hora a verificar
     * @return true se a data/hora já passou
     */
    public static boolean isExpired(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Calcula quantos dias faltam até uma data futura
     * @param futureDate Data futura
     * @return Número de dias (negativo se a data já passou)
     */
    public static long daysUntil(LocalDate futureDate) {
        if (futureDate == null) {
            return 0;
        }
        return daysBetween(LocalDate.now(), futureDate);
    }

    /**
     * Calcula a idade em anos com base em uma data de nascimento
     * @param birthDate Data de nascimento
     * @return Idade em anos
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Retorna uma descrição amigável do tempo decorrido
     * @param dateTime Data/hora passada
     * @return String descritiva (ex: "há 2 dias", "há 5 horas")
     */
    public static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return I18n.get("time.never");
        }

        LocalDateTime now = LocalDateTime.now();

        long days = daysBetween(dateTime, now);
        if (days > 0) {
            return days == 1 ? I18n.get("time.dayAgo", days) : I18n.get("time.daysAgo", days);
        }

        long hours = hoursBetween(dateTime, now);
        if (hours > 0) {
            return hours == 1 ? I18n.get("time.hourAgo", hours) : I18n.get("time.hoursAgo", hours);
        }

        long minutes = minutesBetween(dateTime, now);
        if (minutes > 0) {
            return minutes == 1 ? I18n.get("time.minuteAgo", minutes) : I18n.get("time.minutesAgo", minutes);
        }

        return I18n.get("time.justNow");
    }

    /**
     * Retorna uma descrição amigável do tempo restante até uma data futura
     * @param futureDateTime Data/hora futura
     * @return String descritiva (ex: "em 2 dias", "em 5 horas")
     */
    public static String getTimeUntil(LocalDateTime futureDateTime) {
        if (futureDateTime == null) {
            return "indefinido";
        }

        LocalDateTime now = LocalDateTime.now();

        if (futureDateTime.isBefore(now)) {
            return "expirado";
        }

        long days = daysBetween(now, futureDateTime);
        if (days > 0) {
            return days == 1 ? "em 1 dia" : "em " + days + " dias";
        }

        long hours = hoursBetween(now, futureDateTime);
        if (hours > 0) {
            return hours == 1 ? "em 1 hora" : "em " + hours + " horas";
        }

        long minutes = minutesBetween(now, futureDateTime);
        if (minutes > 0) {
            return minutes == 1 ? "em 1 minuto" : "em " + minutes + " minutos";
        }

        return "agora";
    }

    /**
     * Verifica se uma data está dentro de um intervalo
     * @param date Data a verificar
     * @param startDate Data inicial do intervalo
     * @param endDate Data final do intervalo
     * @return true se a data está no intervalo
     */
    public static boolean isInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Retorna o primeiro dia do mês atual
     * @return Primeiro dia do mês
     */
    public static LocalDate getFirstDayOfMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Retorna o último dia do mês atual
     * @return Último dia do mês
     */
    public static LocalDate getLastDayOfMonth() {
        LocalDate now = LocalDate.now();
        return now.withDayOfMonth(now.lengthOfMonth());
    }

    /**
     * Converte LocalDateTime para timestamp string (yyyyMMddHHmmss)
     * @param dateTime Data/hora a converter
     * @return String no formato timestamp
     */
    public static String toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIMESTAMP_FORMAT);
    }

    /**
     * Retorna a data/hora atual formatada no padrão brasileiro
     * @return String com data/hora atual
     */
    public static String getCurrentDateTimeBR() {
        return formatDateTimeBR(LocalDateTime.now());
    }

    /**
     * Retorna a data atual formatada no padrão brasileiro
     * @return String com data atual
     */
    public static String getCurrentDateBR() {
        return formatDateBR(LocalDate.now());
    }

    /**
     * Retorna a hora atual formatada
     * @return String com hora atual
     */
    public static String getCurrentTime() {
        return formatTime(LocalDateTime.now());
    }
}
