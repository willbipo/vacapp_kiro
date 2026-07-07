package mx.vacapp.infrastructure.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Logback Layout que redacta automáticamente datos sensibles en los logs.
 * 
 * Reemplaza valores de campos sensibles (password, secret, token, etc.) con [REDACTED]
 * para cumplir con los requisitos de seguridad (Requirement 11).
 * 
 * Uso en logback-spring.xml:
 * <layout class="mx.vacapp.infrastructure.logging.SensitiveDataMaskingLayout">
 *   <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
 *   <sensitivePattern>password</sensitivePattern>
 *   <sensitivePattern>secret</sensitivePattern>
 * </layout>
 */
public class SensitiveDataMaskingLayout extends PatternLayout {
    
    private static final String REDACTED = "[REDACTED]";
    private static final int TOKEN_VISIBLE_CHARS = 8; // Show first 8 chars of tokens
    
    private final List<String> sensitivePatterns = new ArrayList<>();
    private List<Pattern> compiledPatterns;
    
    /**
     * Agrega un patrón de campo sensible a redactar.
     * Llamado automáticamente por Logback desde la configuración XML.
     */
    public void addSensitivePattern(String pattern) {
        sensitivePatterns.add(pattern.toLowerCase());
    }
    
    @Override
    public void start() {
        super.start();
        compilePatterns();
    }
    
    /**
     * Compila los patrones regex para búsqueda eficiente.
     */
    private void compilePatterns() {
        compiledPatterns = new ArrayList<>();
        
        for (String sensitiveField : sensitivePatterns) {
            // Patrón para JSON: "field":"value" o "field": "value"
            Pattern jsonPattern = Pattern.compile(
                "\"(" + Pattern.quote(sensitiveField) + ")\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE
            );
            compiledPatterns.add(jsonPattern);
            
            // Patrón para key=value en query strings o logs
            Pattern keyValuePattern = Pattern.compile(
                "\\b(" + Pattern.quote(sensitiveField) + ")=([^&\\s,;]+)",
                Pattern.CASE_INSENSITIVE
            );
            compiledPatterns.add(keyValuePattern);
            
            // Patrón para Java toString: field=value o field='value'
            Pattern toStringPattern = Pattern.compile(
                "\\b(" + Pattern.quote(sensitiveField) + ")=([^,\\s)]+)",
                Pattern.CASE_INSENSITIVE
            );
            compiledPatterns.add(toStringPattern);
            
            // Patrón para formato "field: value"
            Pattern colonPattern = Pattern.compile(
                "\\b(" + Pattern.quote(sensitiveField) + "):\\s*([^,\\s)]+)",
                Pattern.CASE_INSENSITIVE
            );
            compiledPatterns.add(colonPattern);
        }
    }
    
    @Override
    public String doLayout(ILoggingEvent event) {
        // Obtener el mensaje formateado original
        String originalMessage = super.doLayout(event);
        
        // Si no hay patrones configurados, retornar sin modificar
        if (compiledPatterns == null || compiledPatterns.isEmpty()) {
            return originalMessage;
        }
        
        // Aplicar redacción de datos sensibles
        return maskSensitiveData(originalMessage);
    }
    
    /**
     * Aplica redacción de datos sensibles al mensaje.
     */
    private String maskSensitiveData(String message) {
        String maskedMessage = message;
        
        for (Pattern pattern : compiledPatterns) {
            Matcher matcher = pattern.matcher(maskedMessage);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String fieldName = matcher.group(1);
                String fieldValue = matcher.group(2);
                
                // Tratamiento especial para tokens: mostrar solo primeros 8 caracteres
                String replacement;
                if (fieldName.toLowerCase().contains("token") || 
                    fieldName.toLowerCase().contains("authorization")) {
                    replacement = maskToken(fieldValue);
                } else {
                    replacement = REDACTED;
                }
                
                // Reconstruir el match con valor redactado
                String redactedMatch = reconstructMatch(matcher.group(0), fieldName, replacement);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(redactedMatch));
            }
            matcher.appendTail(sb);
            maskedMessage = sb.toString();
        }
        
        return maskedMessage;
    }
    
    /**
     * Redacta un token mostrando solo los primeros caracteres.
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= TOKEN_VISIBLE_CHARS) {
            return REDACTED;
        }
        return token.substring(0, TOKEN_VISIBLE_CHARS) + "..." + REDACTED;
    }
    
    /**
     * Reconstruye el match preservando el formato original.
     */
    private String reconstructMatch(String originalMatch, String fieldName, String maskedValue) {
        // JSON format: "field":"value"
        if (originalMatch.startsWith("\"")) {
            return "\"" + fieldName + "\":\"" + maskedValue + "\"";
        }
        
        // Key-value format: field=value
        if (originalMatch.contains("=")) {
            return fieldName + "=" + maskedValue;
        }
        
        // Colon format: field: value
        if (originalMatch.contains(":")) {
            return fieldName + ": " + maskedValue;
        }
        
        // Default
        return fieldName + "=" + maskedValue;
    }
    
    /**
     * Verifica si un campo es sensible basándose en su nombre.
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        for (String pattern : sensitivePatterns) {
            if (lowerFieldName.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
