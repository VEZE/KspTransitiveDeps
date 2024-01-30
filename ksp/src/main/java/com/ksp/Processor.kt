package com.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile

internal class Processor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val declarations = mutableMapOf<String, KSFile?>()
        resolver.getDeclarationsFromPackage(GENERATED_CLASSES_PACKAGE)
            .forEach { declaration ->
                declarations[declaration.simpleName.getShortName()] = declaration.containingFile
                logger.logging("Package with generated source = ${declaration.packageName}")
            }

        if (invoked) {
            // Processors are repeatedly called until there is no new file generated.
            // No need here, generate only one file in one pass.
            return emptyList()
        }
        invoked = true

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = true,
                sources = declarations.values.filterNotNull().toTypedArray()
            ),
            packageName = "generated.files",
            fileName = "modulesRegistry",
            extensionName = "txt",
        )

        file.bufferedWriter().use { writer ->
            writer.write(declarations.keys.joinToString { it + "\n" })
        }

        return emptyList()
    }
}

internal const val GENERATED_CLASSES_PACKAGE = "com.generated"
