export class User {
  public userId!: string;
  public firstname!: string;
  public lastname!: string;
  public username!: string;
  public email!: string;
  public lastLogInDate!: Date | null;
  public lastLogInDateDisplay!: Date | null;
  public joinDate!: Date| null;
  public profileImageUrl!: string;
  public active!: boolean;
  public notLocked!: boolean;
  public role!: string;
  public authorities!: [];

  constructor() {
    this.userId = '';
    this.firstname = '';
    this.lastname = '';
    this.username = '';
    this.email = '';
    this.lastLogInDate = null;
    this.lastLogInDateDisplay = null;
    this.joinDate = null;
    this.active = false;
    this.notLocked = false;
    this.role = '';
    this.authorities = [];
  }
}
