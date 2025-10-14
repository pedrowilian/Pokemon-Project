package demo;

import utils.DateUtils;
import utils.PasswordUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Classe de demonstração das funcionalidades de Manipulação de Senhas e Datas
 * Requisito do Projeto Escolar
 *
 * Execute esta classe para ver exemplos de todas as funcionalidades implementadas
 */
public class ManipulacaoDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("DEMONSTRAÇÃO: MANIPULAÇÃO DE SENHAS E DATAS");
        System.out.println("Requisito do Projeto Escolar - Pokédex");
        System.out.println("=".repeat(80));
        System.out.println();

        demonstrarManipulacaoSenhas();
        System.out.println();
        demonstrarManipulacaoDatas();
    }

    /**
     * Demonstra todas as funcionalidades de manipulação de senhas
     */
    private static void demonstrarManipulacaoSenhas() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    1. MANIPULAÇÃO DE SENHAS                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 1. Geração de Salt
        System.out.println("1.1 Geração de Salt (para criptografia segura)");
        System.out.println("-".repeat(80));
        String salt1 = PasswordUtils.generateSalt();
        String salt2 = PasswordUtils.generateSalt();
        System.out.println("Salt 1: " + salt1);
        System.out.println("Salt 2: " + salt2);
        System.out.println("✓ Cada salt é único e aleatório");
        System.out.println();

        // 2. Criptografia de Senha
        System.out.println("1.2 Criptografia de Senha (SHA-256 com Salt)");
        System.out.println("-".repeat(80));
        String senha = "Pikachu123!";
        try {
            String hash = PasswordUtils.hashPassword(senha, salt1);
            System.out.println("Senha original: " + senha);
            System.out.println("Hash SHA-256:   " + hash);
            System.out.println("✓ Senha criptografada com sucesso");
            System.out.println("✓ Impossível recuperar senha original do hash");

            // 3. Verificação de Senha
            System.out.println();
            System.out.println("1.3 Verificação de Senha");
            System.out.println("-".repeat(80));
            boolean senhaCorreta = PasswordUtils.verifyPassword("Pikachu123!", hash, salt1);
            boolean senhaIncorreta = PasswordUtils.verifyPassword("pikachu123!", hash, salt1);
            System.out.println("Verificando 'Pikachu123!': " + (senhaCorreta ? "✓ CORRETO" : "✗ INCORRETO"));
            System.out.println("Verificando 'pikachu123!': " + (senhaIncorreta ? "✓ CORRETO" : "✗ INCORRETO"));
            System.out.println("✓ Sistema diferencia maiúsculas/minúsculas");
        } catch (Exception e) {
            System.out.println("✗ Erro: " + e.getMessage());
        }
        System.out.println();

        // 4. Validação de Senha Forte
        System.out.println("1.4 Validação de Senha Forte");
        System.out.println("-".repeat(80));
        String[] senhasTeste = {
            "123456",           // Fraca
            "password",         // Fraca
            "Password1",        // Média (falta caractere especial)
            "Pass123!",         // Boa (mas curta)
            "Pikachu123!",      // Forte
            "MyP@ssw0rd2024"    // Muito Forte
        };

        for (String s : senhasTeste) {
            boolean forte = PasswordUtils.isStrongPassword(s);
            int forca = PasswordUtils.calculatePasswordStrength(s);
            String descricao = PasswordUtils.getPasswordStrengthDescription(forca);
            System.out.printf("%-20s | Forte: %-5s | Força: %3d%% | %s%n",
                s, (forte ? "✓" : "✗"), forca, descricao);
        }
        System.out.println();

        // 5. Sugestões de Melhoria
        System.out.println("1.5 Sugestões para Senha Fraca");
        System.out.println("-".repeat(80));
        String senhaFraca = "senha123";
        System.out.println("Senha: " + senhaFraca);
        System.out.println("Sugestões:");
        System.out.println(PasswordUtils.getPasswordStrengthSuggestions(senhaFraca));

        // 6. Geração de Senha Aleatória
        System.out.println("1.6 Geração de Senha Aleatória Forte");
        System.out.println("-".repeat(80));
        for (int i = 0; i < 3; i++) {
            String senhaAleatoria = PasswordUtils.generateRandomPassword(12);
            int forca = PasswordUtils.calculatePasswordStrength(senhaAleatoria);
            System.out.printf("Senha gerada: %-16s | Força: %3d%% | %s%n",
                senhaAleatoria, forca,
                PasswordUtils.getPasswordStrengthDescription(forca));
        }
        System.out.println("✓ Todas as senhas geradas são fortes automaticamente");
    }

    /**
     * Demonstra todas as funcionalidades de manipulação de datas
     */
    private static void demonstrarManipulacaoDatas() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     2. MANIPULAÇÃO DE DATAS                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 1. Formatação de Datas
        System.out.println("2.1 Formatação de Datas");
        System.out.println("-".repeat(80));
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();
        System.out.println("Data atual (BR):       " + DateUtils.formatDateBR(hoje));
        System.out.println("Data/Hora atual (BR):  " + DateUtils.formatDateTimeBR(agora));
        System.out.println("Hora atual:            " + DateUtils.formatTime(agora));
        System.out.println("Timestamp:             " + DateUtils.toTimestamp(agora));
        System.out.println();

        // 2. Cálculos com Datas
        System.out.println("2.2 Cálculos com Datas");
        System.out.println("-".repeat(80));
        LocalDate dataCriacao = LocalDate.of(2024, 1, 15);
        LocalDateTime ultimoLogin = LocalDateTime.now().minusDays(3).minusHours(5);

        System.out.println("Data de criação da conta: " + DateUtils.formatDateBR(dataCriacao));
        System.out.println("Dias desde criação:       " + DateUtils.daysBetween(dataCriacao, hoje) + " dias");
        System.out.println("Idade da conta:           " + DateUtils.daysBetween(dataCriacao, hoje) + " dias");
        System.out.println();
        System.out.println("Último login:             " + DateUtils.formatDateTimeBR(ultimoLogin));
        System.out.println("Tempo desde último login: " + DateUtils.getTimeAgo(ultimoLogin));
        System.out.println("Horas desde último login: " + DateUtils.hoursBetween(ultimoLogin, agora) + " horas");
        System.out.println();

        // 3. Descrições Amigáveis de Tempo
        System.out.println("2.3 Descrições Amigáveis de Tempo");
        System.out.println("-".repeat(80));
        LocalDateTime[] momentos = {
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().minusMinutes(45),
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(6),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now().minusDays(30)
        };

        for (LocalDateTime momento : momentos) {
            System.out.printf("%-25s -> %s%n",
                DateUtils.formatDateTimeBR(momento),
                DateUtils.getTimeAgo(momento));
        }
        System.out.println();

        // 4. Datas Futuras
        System.out.println("2.4 Cálculos com Datas Futuras");
        System.out.println("-".repeat(80));
        LocalDate em7Dias = DateUtils.addDays(hoje, 7);
        LocalDate em30Dias = DateUtils.addDays(hoje, 30);
        LocalDate em90Dias = DateUtils.addDays(hoje, 90);

        System.out.println("Hoje:          " + DateUtils.formatDateBR(hoje));
        System.out.println("Em 7 dias:     " + DateUtils.formatDateBR(em7Dias) +
                           " (" + DateUtils.daysUntil(em7Dias) + " dias restantes)");
        System.out.println("Em 30 dias:    " + DateUtils.formatDateBR(em30Dias) +
                           " (" + DateUtils.daysUntil(em30Dias) + " dias restantes)");
        System.out.println("Em 90 dias:    " + DateUtils.formatDateBR(em90Dias) +
                           " (" + DateUtils.daysUntil(em90Dias) + " dias restantes)");
        System.out.println();

        // 5. Validações Temporais
        System.out.println("2.5 Validações Temporais");
        System.out.println("-".repeat(80));
        LocalDate dataPassada = LocalDate.now().minusDays(10);
        LocalDate dataFutura = LocalDate.now().plusDays(10);

        System.out.println("Data passada:  " + DateUtils.formatDateBR(dataPassada) +
                           " -> Expirada? " + (DateUtils.isExpired(dataPassada) ? "✓ SIM" : "✗ NÃO"));
        System.out.println("Data futura:   " + DateUtils.formatDateBR(dataFutura) +
                           " -> Expirada? " + (DateUtils.isExpired(dataFutura) ? "✓ SIM" : "✗ NÃO"));
        System.out.println();

        // 6. Intervalos de Data
        System.out.println("2.6 Verificação de Intervalos");
        System.out.println("-".repeat(80));
        LocalDate inicioMes = DateUtils.getFirstDayOfMonth();
        LocalDate fimMes = DateUtils.getLastDayOfMonth();

        System.out.println("Primeiro dia do mês: " + DateUtils.formatDateBR(inicioMes));
        System.out.println("Último dia do mês:   " + DateUtils.formatDateBR(fimMes));
        System.out.println("Hoje está no mês?    " +
                           (DateUtils.isInRange(hoje, inicioMes, fimMes) ? "✓ SIM" : "✗ NÃO"));
        System.out.println();

        // 7. Cálculo de Idade
        System.out.println("2.7 Cálculo de Idade");
        System.out.println("-".repeat(80));
        LocalDate nascimento1 = LocalDate.of(2000, 5, 15);
        LocalDate nascimento2 = LocalDate.of(1995, 12, 25);

        System.out.println("Nascimento: " + DateUtils.formatDateBR(nascimento1) +
                           " -> Idade: " + DateUtils.calculateAge(nascimento1) + " anos");
        System.out.println("Nascimento: " + DateUtils.formatDateBR(nascimento2) +
                           " -> Idade: " + DateUtils.calculateAge(nascimento2) + " anos");

        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("✓ TODAS AS FUNCIONALIDADES DEMONSTRADAS COM SUCESSO");
        System.out.println("=".repeat(80));
    }
}
