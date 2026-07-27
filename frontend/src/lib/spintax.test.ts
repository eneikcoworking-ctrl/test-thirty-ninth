import { describe, it, expect } from 'vitest';
import { parseSpintax, calculatePermutations, generateVariation, createPRNG } from './spintax';

describe('Spintax Utility', () => {
  it('correctly parses spintax blocks', () => {
    const template = 'Hello {first_name|there}, interested in {industry|SaaS}?';
    const blocks = parseSpintax(template);
    expect(blocks.length).toBe(2);
    expect(blocks[0].raw).toBe('{first_name|there}');
    expect(blocks[0].options).toEqual(['first_name', 'there']);
    expect(blocks[1].raw).toBe('{industry|SaaS}');
    expect(blocks[1].options).toEqual(['industry', 'SaaS']);
  });

  it('calculates permutations correctly', () => {
    const blocks = [
      { raw: '{a|b}', options: ['a', 'b'] },
      { raw: '{c|d|e}', options: ['c', 'd', 'e'] }
    ];
    expect(calculatePermutations(blocks)).toBe(6);
  });

  it('returns 0 permutations if no blocks are present', () => {
    expect(calculatePermutations([])).toBe(0);
  });

  it('generates reproducible variation when using createPRNG with seed', () => {
    const template = 'Hello {first_name|there}, interested in {industry|SaaS}?';

    // Seed 12345
    const prng1 = createPRNG(12345);
    const var1 = generateVariation(template, prng1);

    // Reset seed 12345
    const prng2 = createPRNG(12345);
    const var2 = generateVariation(template, prng2);

    expect(var1).toBe(var2);
    // Since seed 12345 has a known LCG output sequence, we verify it is deterministic
    expect(var1).toBe('Hello first_name, interested in industry?');
  });
});
