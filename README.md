# AlkaCrates

> Sistema completo de crates: chaves virtuais, animações, pity system e recompensas configuráveis

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.9-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## 📋 Sobre o Projeto

O **AlkaCrates** é o sistema de caixas premiadas da rede AlkaStudio: crates
físicas ou virtuais, com animação de abertura, chances configuráveis com
sistema de pity (garantia progressiva de prêmio raro), chaves virtuais e uma
variedade de tipos de recompensa — tudo administrável via GUI.

## ✨ Funcionalidades Principais

- 🎁 **Múltiplos motores visuais de crate**: baú vanilla, baú físico, ou
  integração com ModelEngine, BetterModel, CraftEngine e ItemsAdder para
  crates customizadas no mundo.
- 🎬 **Animação de abertura**: sequência visual completa ao abrir uma crate.
- 🔑 **Chaves virtuais**: sem depender de item físico no inventário.
- 🎯 **Sistema de pity**: aumenta progressivamente a chance de prêmios raros
  a cada abertura sem sorte, com precisão de chance configurável até
  frações bem pequenas.
- 🏆 **Recompensas variadas**: comando, item, kit, dinheiro, permissão
  temporária ou VIP.
- 🖥️ **GUI de administração**: criação e edição de crates sem precisar
  editar YAML na mão.
- 👁️ **Preview de prêmios**: menu que mostra ao jogador os prêmios possíveis
  antes de abrir.

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/crate` (`crates`) | Comandos de crates para jogadores | `alkacrates.use` |
| `/alkacrates` (`acrates`, `alkacrate`) | Comandos administrativos de crates | `alkacrates.admin` |

## 🔗 Integrações

Construído sobre o **AlkaCore** e o **AlkaEconomy**. Suporte visual
opcional a **ItemsAdder**, **ModelEngine**, **BetterModel**, **CraftEngine**
e **Nexo**, recompensas de item via **MMOItems**/**ExecutableItems**,
ícones via **HeadDatabase**, e hooks para **PlaceholderAPI**, **LuckPerms**
e **MythicMobs**.

## 🔧 Tecnologias Utilizadas

- **Java 21**
- **Paper API 1.21.8** (Folia-ready)
- **AlkaCore** + **AlkaEconomy**
- **MiniMessage** para formatação de texto

## ⚙️ Instalação

1. Baixe o `AlkaCrates.jar` mais recente.
2. Coloque na pasta `plugins/` do servidor.
3. Certifique-se de ter **AlkaCore** e **AlkaEconomy** instalados (dependências obrigatórias).
4. Reinicie o servidor.
5. Configure suas crates em `plugins/AlkaCrates/crates/` ou pela GUI de administração.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `alkacrates.use` | Permite abrir crates | true |
| `alkacrates.admin` | Acesso administrativo total ao AlkaCrates | op |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
