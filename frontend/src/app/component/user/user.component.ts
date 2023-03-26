import {Component, OnDestroy, OnInit} from '@angular/core';
import {BehaviorSubject, Subscription} from "rxjs";
import {User} from "../../model/User";
import {UserService} from "../../service/user.service";
import {NotificationService} from "../../service/notification.service";
import {HttpErrorResponse, HttpEvent, HttpEventType} from "@angular/common/http";
import {NotificationType} from "../../enum/notification-type";
import {NgForm} from "@angular/forms";
import {CustomHttpResponse} from "../../model/custom-http-response";
import {AuthenticationService} from "../../service/authentication.service";
import {Router} from "@angular/router";
import {FileUploadStatus} from "../../model/file_upload.status";
import {Role} from "../../enum/role.enum";
import {SubSink} from "subsink";

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit, OnDestroy {

  private subs = new SubSink();
  private titleSubject: BehaviorSubject<string> = new BehaviorSubject<string>('Users');
  public titleAction$ = this.titleSubject.asObservable();
  public users: User[] | null = [];
  public refreshing: boolean = false;
  public selectedUser!: User | null;
  fileName!: string | null;
  private profileImage!: File | null;
  public editUser: User = new User();
  public currentUserName!: string;
  public user!: User;
  public fileStatus:FileUploadStatus = new FileUploadStatus();

  constructor(private router: Router, private authenticationService: AuthenticationService, private userService: UserService, private notificationService: NotificationService) {
  }

  public getUsers(showNotification: boolean): void {
    this.refreshing = true;
    this.subs.add(
      this.userService.getUsers().subscribe(
        (response: User[]) => {
          this.userService.addUserToLocalCache(response);
          this.users = response;
          this.refreshing = false;
          if (showNotification) {
            this.sendNotification(NotificationType.SUCCESS, `${response.length} user(s) loaded successfully.`);
          }
        },
        ((httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
          this.refreshing = false;
        })
      )
    )
  }

  private sendNotification(notificationType: NotificationType, message: string): void {
    if (message) {
      this.notificationService.notify(notificationType, message);
    } else {
      this.notificationService.notify(notificationType, 'AN ERROR OCCURRED, PLEASE TRY AGAIN');
    }
  }

  public changeTitle(title: string): void {
    this.titleSubject.next(title);
  }

  ngOnInit(): void {
    this.user = this.authenticationService.getUserFromLocalCache();
    this.getUsers(true);
  }

  public onSelectUser(selectedUser: User): void {
    this.selectedUser = selectedUser;
    // @ts-ignore
    this.clickButton('openUserInfo');
  }


  onProfileImageChange(fileName: string, profileImage: File) {
    this.fileName = fileName;
    this.profileImage = profileImage;
  }

  public onAddNewUser(userForm: NgForm): void {
    const formData = this.userService.createUserFormData(null, userForm.value, this.profileImage);
    this.subs.add(
      this.userService.addUser(formData).subscribe(
        (response: User) => {
          this.clickButton('hide-modal-add-user');
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          userForm.reset();
          this.sendNotification(NotificationType.SUCCESS, `${response.firstname} ${response.lastname} added successfully`);
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
          this.profileImage = null;
        }
      )
    )
  }

  private clickButton(id: string): void {
    // @ts-ignore
    document.getElementById(id).click();
  }

  public searchUsers(searchTerm: string): void {
    const results: User[] = [];
    // @ts-ignore
    for (const user of this.userService.getUsersFromLocalCache()) {
      if (user.firstname.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 ||
        user.lastname.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 ||
        user.username.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 ||
        user.userId.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1) {
        results.push(user);
      }
    }
    this.users = results;
    if (results.length === 0 || !searchTerm) {
      this.users = this.userService.getUsersFromLocalCache();
    }
  }

  public onEditUser(editUser: User): void {
    this.editUser = editUser;
    this.currentUserName = editUser.username;
    this.clickButton('openEditUser');
  }

  public onUpdateUser(): void {
    const formData = this.userService.createUserFormData(this.currentUserName, this.editUser, this.profileImage);
    this.subs.add(
      this.userService.updateUser(formData).subscribe(
        (response: User) => {
          this.clickButton('hide-modal-edit-user');
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          this.sendNotification(NotificationType.SUCCESS, `${response.firstname} ${response.lastname} updated successfully`);
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
          this.profileImage = null;
        }
      )
    );
  }

  public onDeleteUser(username: string): void {
    this.subs.add(
      this.userService.deleteUser(username).subscribe(
        (response: CustomHttpResponse) => {
          this.sendNotification(NotificationType.SUCCESS, response.message);
          this.getUsers(false);
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
        }
      )
    );
  }

  public onResetPassword(emailFrom: NgForm): void {
    this.refreshing = true;
    const emailAddress = emailFrom.value['reset-password-email'];
    this.subs.add(
      this.userService.resetPassword(emailAddress).subscribe(
        (response: CustomHttpResponse) => {
          this.sendNotification(NotificationType.SUCCESS, response.message);
          this.refreshing = false;
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.WARNING, httpErrorResponse.error.message);
          this.refreshing = false;
        },
        () => emailFrom.reset()
      )
    )
  }

  public onLogOut(): void {
    this.authenticationService.logOut();
    this.router.navigate(['/login']);
    this.sendNotification(NotificationType.SUCCESS, `You been successfully logged out `);
  }

  public onUpdateCurrentUser(user: User): void {
    // @ts-ignore
    this.currentUserName = this.authenticationService.getUserFromLocalCache().username;
    const formData = this.userService.createUserFormData(this.currentUserName, user, this.profileImage);
    this.subs.add(
      this.userService.updateUser(formData).subscribe(
        (response: User) => {
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          this.sendNotification(NotificationType.SUCCESS, `${response.firstname} ${response.lastname} updated successfully`);
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
          this.profileImage = null;
        }
      )
    );
  }

  public onUpdateProfileImage(): void {
    const formData = new FormData();
    formData.append('username', this.user.username);
    formData.append('profileImageUrl', this.user.profileImageUrl);
    this.subs.add(
      this.userService.updateProfileImage(formData).subscribe(
        (event: HttpEvent<any>) => {
          this.reportUploadProgress(event);
        },
        (httpErrorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, httpErrorResponse.error.message);
          this.fileStatus.status = 'done';
        }
      )
    );
  }

  private reportUploadProgress(event: HttpEvent<any>): void {
    switch (event.type) {
      case HttpEventType.UploadProgress:
        // @ts-ignore
        this.fileStatus.percentage = Math.round(100 * event.loaded / event.total);
        this.fileStatus.status = 'progress';
        break;

      case HttpEventType.Response:
        if (event.status === 200) {
          this.user.profileImageUrl = `${event.body.profileImageUrl}?time=${new Date().getTime()}`;
          this.sendNotification(NotificationType.SUCCESS, `${event.body.firstname}\'s profile image updated successfully`);
          this.fileStatus.status = 'done';
          break;
        }
        else {
          this.sendNotification(NotificationType.SUCCESS, `Unable to upload image please try again`);
          break;
        }
      default:
        `Finished all processes`;
    }
  }

  private getUserRole(): string {
    return this.authenticationService.getUserFromLocalCache().role;
  }

  public get isAdmin(): boolean {
    return this.getUserRole() === Role.ADMIN || this.getUserRole() === Role.SUPER_ADMIN;
  }

  public get isManager(): boolean {
    return this.isAdmin || this.getUserRole() === Role.MANAGER;
  }

  public get isAdminOrManager(): boolean {
    return this.isAdmin || this.isManager;
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe()
  }
}
