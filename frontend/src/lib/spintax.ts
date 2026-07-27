/**
 * Seedable Pseudo-Random Number Generator (PRNG) using LCG.
 * Returns a function that generates a pseudo-random float between 0 (inclusive) and 1 (exclusive).
 */
export function createPRNG(seed: number): () => number {
  let s = seed;
  return () => {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

export interface SpintaxBlock {
  raw: string;
  options: string[];
}

/**
 * Parses all spintax blocks (i.e. content inside curly braces) from a template string.
 */
export function parseSpintax(text: string): SpintaxBlock[] {
  const regex = /\{([^{}]+)\}/g;
  const blocks: SpintaxBlock[] = [];
  let match;

  while ((match = regex.exec(text)) !== null) {
    const raw = match[0];
    const inner = match[1];
    const options = inner.split('|').map(opt => opt.trim());
    blocks.push({ raw, options });
  }

  return blocks;
}

/**
 * Calculates the total number of unique permutations from parsed spintax blocks.
 */
export function calculatePermutations(blocks: SpintaxBlock[]): number {
  if (blocks.length === 0) return 0;
  return blocks.reduce((acc, block) => acc * block.options.length, 1);
}

/**
 * Generates a single resolved variation from the template text using the provided random function.
 */
export function generateVariation(text: string, randomFn: () => number = Math.random): string {
  const regex = /\{([^{}]+)\}/g;
  return text.replace(regex, (_, inner) => {
    const options = inner.split('|').map((opt: string) => opt.trim());
    if (options.length === 0) return '';
    const index = Math.floor(randomFn() * options.length);
    return options[index];
  });
}
