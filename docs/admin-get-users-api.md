# Admin Get Users API

## Get Users

Returns a paginated list of users for the admin panel.

```http
GET /admin/get-users?page=0&size=25
```

## Authentication

This endpoint is under `/admin/**`, so the request must be sent with an admin or maintainer JWT.

```http
Authorization: Bearer <jwt_token>
```

Allowed roles:

- `ADMIN`
- `MAINTAINER`

## Query Parameters

| Parameter | Type | Required | Default | Max | Description |
| --- | --- | --- | --- | --- | --- |
| `page` | integer | No | `0` | - | Page number. Starts from `0`. |
| `size` | integer | No | `25` | `100` | Number of users to return in one API call. |

If the client sends `size` greater than `100`, the backend still returns only `100` users.

Users are returned in descending order of `created_at`, so newest users come first.

## Request Examples

First 25 users:

```http
GET /admin/get-users?page=0&size=25
Authorization: Bearer <jwt_token>
```

Next 25 users:

```http
GET /admin/get-users?page=1&size=25
Authorization: Bearer <jwt_token>
```

For 100 users with `size=25`:

| Request | Returned users |
| --- | --- |
| `page=0&size=25` | Users 1-25 |
| `page=1&size=25` | Users 26-50 |
| `page=2&size=25` | Users 51-75 |
| `page=3&size=25` | Users 76-100 |

## Success Response

```json
{
  "users": [
    {
      "id": 1,
      "employee_name": "Rahul Sharma",
      "employee_id": "EMP123",
      "phone_number": "9876543210",
      "is_expired": false,
      "created_at": "2026-07-02T20:22:10.424",
      "expires_at": "2026-08-02T20:22:10.424",
      "role": "USER",
      "total_booked_slots": 12,
      "user_id": "123456",
      "api_key": "user-api-key",
      "site_id": "SITE001"
    }
  ],
  "page": 0,
  "size": 25,
  "total_pages": 4,
  "total_users": 100,
  "has_next": true
}
```

## Response Fields

| Field | Type | Description |
| --- | --- | --- |
| `users` | array | List of users for the requested page. |
| `page` | integer | Current page number. Starts from `0`. |
| `size` | integer | Page size used by backend. |
| `total_pages` | integer | Total available pages. |
| `total_users` | integer | Total users in the database. |
| `has_next` | boolean | `true` if another page is available. |

## User Object Fields

| Field | Type | Source | Description |
| --- | --- | --- | --- |
| `id` | number | `UserModel.id` | Database user ID. |
| `employee_name` | string/null | `UserHeaderModel.employeeName` | Employee name from saved user headers. |
| `employee_id` | string/null | `UserHeaderModel.employeeId` | Employee ID from saved user headers. |
| `phone_number` | string/null | `UserModel.phone` | User phone number. |
| `is_expired` | boolean/null | `UserModel.expired` | Whether the user plan/session is expired. |
| `created_at` | string/null | `UserModel.createdAt` | User creation date and time. |
| `expires_at` | string/null | `UserModel.expiresAt` | User expiry date and time. |
| `role` | string | `UserModel.role` | User role, for example `USER`, `ADMIN`, or `MAINTAINER`. |
| `total_booked_slots` | number | `UserModel.totalBookedSlots` | Total slots booked by this user. Returns `0` when empty. |
| `user_id` | string/null | `UserHeaderModel.userId` | Blinkit/user header ID. |
| `api_key` | string/null | `UserModel.apiKey` | User API key. |
| `site_id` | string/null | `UserHeaderModel.siteId` | Site ID from saved user headers. |

## Error Responses

If the token is missing or invalid:

```http
401 Unauthorized
```

If the logged-in user is not `ADMIN` or `MAINTAINER`:

```http
403 Forbidden
```

# Admin Search User API

## Search User By Phone

Returns a user by phone number. The response shape is the same as `GET /admin/get-users`.

```http
GET /admin/search-user?phone=9876543210
```

## Authentication

This endpoint is under `/admin/**`, so the request must be sent with an admin or maintainer JWT.

```http
Authorization: Bearer <jwt_token>
```

Allowed roles:

- `ADMIN`
- `MAINTAINER`

## Query Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `phone` | string | Yes | Phone number to search. |

## Request Example

```http
GET /admin/search-user?phone=9876543210
Authorization: Bearer <jwt_token>
```

## Success Response

When a user is found:

```json
{
  "users": [
    {
      "id": 1,
      "employee_name": "Rahul Sharma",
      "employee_id": "EMP123",
      "phone_number": "9876543210",
      "is_expired": false,
      "created_at": "2026-07-02T20:22:10.424",
      "expires_at": "2026-08-02T20:22:10.424",
      "role": "USER",
      "total_booked_slots": 12,
      "user_id": "123456",
      "api_key": "user-api-key",
      "site_id": "SITE001"
    }
  ],
  "page": 0,
  "size": 1,
  "total_pages": 1,
  "total_users": 1,
  "has_next": false
}
```

When no user is found:

```json
{
  "users": [],
  "page": 0,
  "size": 0,
  "total_pages": 0,
  "total_users": 0,
  "has_next": false
}
```
