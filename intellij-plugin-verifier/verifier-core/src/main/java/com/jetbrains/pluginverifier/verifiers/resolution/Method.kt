package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.MethodLocation
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.TryCatchBlockNode

interface Method : ClassFileMember {
  override val location: MethodLocation
  override val containingClassFile: ClassFile

  val owner: ClassFile
  val name: String
  val descriptor: String
  val signature: String?
  val accessType: AccessType
  val exceptions: List<String>

  val isAbstract: Boolean
  val isStatic: Boolean
  val isFinal: Boolean
  val isPublic: Boolean
  val isProtected: Boolean
  val isPrivate: Boolean
  val isDefaultAccess: Boolean
  val isDeprecated: Boolean
  val isVararg: Boolean
  val isNative: Boolean
  val isSynthetic: Boolean
  val isBridgeMethod: Boolean

  //ASM-specific classes are returned, to avoid mirroring of ASM classes. May be abstracted away of ASM, if necessary.
  val instructions: List<AbstractInsnNode>
  val tryCatchBlocks: List<TryCatchBlockNode>
  val localVariables: List<LocalVariableNode>
  val invisibleAnnotations: List<AnnotationNode>
}