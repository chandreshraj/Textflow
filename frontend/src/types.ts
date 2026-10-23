export interface Parameter { id: string; label: string; type: 'string' | 'multiline' | 'number' | 'boolean' | 'enum'; required: boolean; defaultValue: string | number | boolean | null; min: number | null; max: number | null; options: string[]; help: string }
export interface Descriptor { id: string; name: string; description: string; parameters: Parameter[] }
export type Value = string | number | boolean | null
export interface Step { key: number; transformation: string; parameters: Record<string, Value> }
export interface Result { output: string; steps: { index: number; transformation: string; output: string }[] }
export interface ApiError { message: string; step?: number | null; parameter?: string | null }
