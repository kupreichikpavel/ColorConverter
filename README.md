# Проектирование компьютерной графики — лабораторные работы

Каждая лабораторная — отдельный Gradle-подпроект в своей папке.
Gradle Wrapper и CI общие для всего репозитория.

| № | Тема | Папка |
|---|------|-------|
| 1 | Цветовые модели (CMYK ↔ LAB ↔ RGB) | [lab1-color-converter](lab1-color-converter) |

## Сборка и запуск

    ./gradlew :lab1-color-converter:run   # запуск конкретной лабы
    ./gradlew test                        # тесты всех лаб

Windows-установщики (.exe) собираются GitHub Actions
(workflow «Build Windows EXE») и лежат в артефактах запуска.
