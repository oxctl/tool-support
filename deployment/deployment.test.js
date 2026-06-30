import { test, expect } from '@playwright/test'
import 'dotenv/config'

test('smoke check: JWKS endpoint returns a valid JWK Set', async ({ request }) => {
  const url = process.env.URL
  if (!url) {
    throw new Error("URL not configured.")
  }

  const jwksUrl = new URL('/.well-known/jwks.json', url).toString()

  const response = await request.get(jwksUrl)
  expect(response.ok()).toBeTruthy()

  const body = await response.json()
  expect(body).toEqual(expect.objectContaining({ keys: expect.any(Array) }))
  expect(body.keys.length).toBeGreaterThan(0)

  for (const key of body.keys) {
    expect(key).toEqual(expect.objectContaining({
      kty: 'RSA',
      kid: expect.any(String),
      use: 'sig',
      alg: 'RS256',
      n: expect.any(String),
      e: expect.any(String),
    }))

    expect(key.n).toEqual(expect.any(String))
    expect(key.e).toEqual(expect.any(String))
  }
})
