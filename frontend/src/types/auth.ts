export interface UserProfile {
  id: number;
  username: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  userProfileResponse: UserProfile;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface SignupPayload extends LoginPayload {
  name: string;
}
